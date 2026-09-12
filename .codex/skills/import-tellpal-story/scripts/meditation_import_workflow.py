from __future__ import annotations

from collections.abc import Iterable

from meditation_import_report import MeditationImportRunReport
from meditation_manifest import (
    MeditationGroupPlan,
    MeditationPlan,
    assert_source_unchanged,
)
from tellpal_admin_client import TellPalAdminClient


class MeditationRemotePreflightError(RuntimeError):
    """Raised after authentication but before the first meditation mutation."""


def format_preview(plan: MeditationPlan, *, api_base_url: str | None = None) -> str:
    languages = sorted(
        {
            localization.language_code
            for group in plan.groups
            for localization in group.localizations
        }
    )
    lines = [
        "Meditation import preview",
        f"  CSV: {plan.csv_path}",
        f"  Storage: {plan.storage_bucket}/{plan.cover_prefix} + /{plan.audio_prefix or '<root>'}",
        f"  API: {api_base_url or '(local preflight only)'}",
        f"  Groups: {len(plan.groups)}",
        f"  Languages: {', '.join(languages)}",
        f"  Media uploads: {plan.expected_actions['media_uploads']}",
        f"  Content localizations: {plan.expected_actions['localizations']}",
        f"  Publications: {plan.expected_actions['publications']}",
        f"  Active: {str(plan.active).lower()}",
        f"  Source fingerprint: {plan.source_fingerprint}",
        "  Groups:",
    ]
    for group in plan.groups:
        members = ", ".join(
            f"{item.language_code}(id={item.legacy_id}, line={item.line_number})"
            for item in group.localizations
        )
        lines.append(
            f"    - {group.external_key}: {members}; cover={group.listening_cover_name}; "
            f"coverMedia={group.listening_cover_media_key}"
        )
        for localization in group.localizations:
            body_state = "provided" if localization.body_text else "missing"
            lines.append(
                f"      {localization.language_code}: title={localization.title!r}, "
                f"duration={localization.duration_minutes} min, body={body_state}, "
                f"audio={localization.audio_source_name}"
            )
    if plan.missing_body_sources:
        lines.append("  Live import: unavailable (body text required)")
    if plan.warnings:
        lines.append("  Warnings:")
        lines.extend(f"    - {warning}" for warning in plan.warnings)
    return "\n".join(lines)


def remote_preflight(plan: MeditationPlan, client: TellPalAdminClient) -> tuple[()]:
    if plan.missing_body_sources:
        raise MeditationRemotePreflightError(
            "Body text is required before live import; missing: "
            + ", ".join(plan.missing_body_sources)
        )
    existing = client.list_contents()
    planned_keys = {group.external_key for group in plan.groups}
    conflicts = sorted(
        {
            str(item.get("externalKey"))
            for item in existing
            if item.get("externalKey") in planned_keys
        }
    )
    if conflicts:
        raise MeditationRemotePreflightError(
            "External key already exists; automatic update/resume is forbidden: "
            f"{conflicts}"
        )
    return ()


def execute_import(
    plan: MeditationPlan,
    client: TellPalAdminClient,
    report: MeditationImportRunReport,
) -> dict[str, object]:
    if plan.missing_body_sources:
        raise MeditationRemotePreflightError(
            "Body text is required before live import; missing: "
            + ", ".join(plan.missing_body_sources)
        )
    assert_source_unchanged(plan)
    report.mark_running()
    uploaded: dict[str, int] = {}
    content_ids: list[int] = []

    for group in plan.groups:
        report.set_phase("create-content")
        content = client.create_content(
            {
                "type": "MEDITATION",
                "externalKey": group.external_key,
                "ageRange": None,
                "active": plan.active,
            }
        )
        content_id = _positive_int(content.get("contentId"), "contentId")
        content_ids.append(content_id)
        report.add_content_id(content_id)
        report.record_step(f"content-created:{group.external_key}")

        cover_id = _upload_media(plan, client, report, group.listening_cover_media_key, uploaded)
        report.set_phase("attach-listening-cover")
        client.update_content(
            content_id,
            {
                "externalKey": group.external_key,
                "ageRange": None,
                "active": plan.active,
                "textlessCoverMediaId": None,
                "listeningCoverMediaId": cover_id,
                "listingCoverMediaId": None,
            },
        )
        report.record_step(f"listening-cover-attached:{group.external_key}")

        report.set_phase("create-localizations")
        for localization in group.localizations:
            audio_id = _upload_media(plan, client, report, localization.audio_media_key, uploaded)
            client.create_localization(
                content_id,
                localization.language_code,
                {
                    "title": localization.title,
                    "description": localization.description,
                    "bodyText": localization.body_text,
                    "coverMediaId": None,
                    "audioMediaId": audio_id,
                    "durationMinutes": localization.duration_minutes,
                    "status": "DRAFT",
                    "processingStatus": "PENDING",
                    "publishedAt": None,
                },
            )
        report.record_step(f"localizations-created:{group.external_key}")

        report.set_phase("verify-before-publication")
        verify_group(plan, group, content_id, client, uploaded)
        report.record_step(f"pre-publication-verified:{group.external_key}")

        if plan.publish:
            report.set_phase("publish-localizations")
            for localization in group.localizations:
                client.publish_localization(content_id, localization.language_code)
            report.record_step(f"localizations-published:{group.external_key}")
            report.set_phase("verify-final-state")
            verify_group(plan, group, content_id, client, uploaded, published=True)
            report.record_step(f"final-state-verified:{group.external_key}")

    summary = {
        "contentIds": content_ids,
        "externalKeys": [group.external_key for group in plan.groups],
        "groups": len(plan.groups),
        "languages": sorted(
            {
                localization.language_code
                for group in plan.groups
                for localization in group.localizations
            }
        ),
        "publishedLanguages": sorted(
            {
                localization.language_code
                for group in plan.groups
                for localization in group.localizations
            }
        )
        if plan.publish
        else [],
        "mediaUploads": len(uploaded),
    }
    report.mark_success(summary)
    return summary


def verify_group(
    plan: MeditationPlan,
    group: MeditationGroupPlan,
    content_id: int,
    client: TellPalAdminClient,
    uploaded: dict[str, int],
    *,
    published: bool = False,
) -> None:
    content = client.get_content(content_id)
    if not isinstance(content, dict):
        raise RuntimeError(f"Verification failed for {group.external_key}: content response is not an object")
    if (
        content.get("type") != "MEDITATION"
        or content.get("externalKey") != group.external_key
        or content.get("active") is not plan.active
        or content.get("listeningCoverMediaId") != uploaded[group.listening_cover_media_key]
        or content.get("textlessCoverMediaId") is not None
        or content.get("listingCoverMediaId") is not None
    ):
        raise RuntimeError(f"Verification failed for {group.external_key}: content identity or cover mismatch")
    raw_localizations = content.get("localizations")
    if not isinstance(raw_localizations, list):
        raise RuntimeError(
            f"Verification failed for {group.external_key}: localizations response is not a list"
        )
    if any(not isinstance(item, dict) for item in raw_localizations):
        raise RuntimeError(
            f"Verification failed for {group.external_key}: localization entry is not an object"
        )
    localization_languages = [item.get("languageCode") for item in raw_localizations]
    if any(not isinstance(language, str) or not language for language in localization_languages):
        raise RuntimeError(
            f"Verification failed for {group.external_key}: localization language is invalid"
        )
    if len(set(localization_languages)) != len(localization_languages):
        raise RuntimeError(
            f"Verification failed for {group.external_key}: duplicate localization language"
        )
    localizations = {
        language: item
        for language, item in zip(localization_languages, raw_localizations)
    }
    expected_languages = {item.language_code for item in group.localizations}
    if set(localizations) != expected_languages:
        raise RuntimeError(f"Verification failed for {group.external_key}: localization languages mismatch")
    for localization in group.localizations:
        stored = localizations[localization.language_code]
        expected_status = "PUBLISHED" if published else "DRAFT"
        if (
            stored.get("title") != localization.title
            or stored.get("description") != localization.description
            or stored.get("bodyText") != localization.body_text
            or stored.get("coverMediaId") is not None
            or stored.get("audioMediaId") != uploaded[localization.audio_media_key]
            or stored.get("durationMinutes") != localization.duration_minutes
            or stored.get("status") != expected_status
            or stored.get("processingStatus") not in {"PENDING", "PROCESSING", "COMPLETED"}
        ):
            raise RuntimeError(
                f"Verification failed for {group.external_key}/{localization.language_code}: localization mismatch"
            )
    for media_key in {
        group.listening_cover_media_key,
        *(localization.audio_media_key for localization in group.localizations),
    }:
        media = plan.media_by_key(media_key)
        stored = client.get_media(uploaded[media_key])
        if (
            stored.get("assetId") != uploaded[media_key]
            or stored.get("byteSize") != media.byte_size
            or stored.get("checksumSha256") != media.checksum_sha256
        ):
            raise RuntimeError(f"Verification failed for media {media.source_name}: stored asset mismatch")


def _upload_media(
    plan: MeditationPlan,
    client: TellPalAdminClient,
    report: MeditationImportRunReport,
    media_key: str,
    uploaded: dict[str, int],
) -> int:
    if media_key in uploaded:
        return uploaded[media_key]
    media = plan.media_by_key(media_key)
    response = client.upload_media(media.local_path, media.kind, media.checksum_sha256)
    asset_id = _positive_int(response.get("assetId"), "assetId")
    uploaded[media_key] = asset_id
    report.record_asset(
        asset_id=asset_id,
        media_key=media_key,
        source_name=media.source_name,
        kind=media.kind,
        checksum_sha256=media.checksum_sha256,
        byte_size=media.byte_size,
    )
    return asset_id


def _positive_int(value: object, field: str) -> int:
    if isinstance(value, bool) or not isinstance(value, int) or value <= 0:
        raise RuntimeError(f"API response field {field} must be a positive integer; received {value!r}")
    return value
