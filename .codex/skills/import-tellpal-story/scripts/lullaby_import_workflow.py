from __future__ import annotations

from collections.abc import Iterable

from lullaby_import_report import LullabyImportRunReport
from lullaby_manifest import LullabyGroupPlan, LullabyPlan, assert_source_unchanged
from story_import_models import ContributorResolution
from tellpal_admin_client import TellPalAdminClient


class LullabyRemotePreflightError(RuntimeError):
    """Raised after authentication but before the first lullaby mutation."""


def format_preview(plan: LullabyPlan, *, api_base_url: str | None = None, contributor_resolutions: Iterable[ContributorResolution] = ()) -> str:
    lines = [
        "Lullaby import preview",
        f"  CSV: {plan.csv_path}",
        f"  Storage: {plan.storage_bucket}/{plan.cover_prefix} + /{plan.audio_prefix or '<root>'}",
        f"  Groups: {len(plan.groups)}",
        f"  Languages: {', '.join(sorted({localization.language_code for group in plan.groups for localization in group.localizations}))}",
        f"  Media uploads: {plan.expected_actions['media_uploads']}",
        f"  Publications: {plan.expected_actions['publications']}",
        f"  Active: {str(plan.active).lower()}",
    ]
    if api_base_url:
        lines.append(f"  API: {api_base_url}")
    for group in plan.groups:
        lines.append(
            f"  - {group.external_key}: {','.join(localization.language_code for localization in group.localizations)}; "
            f"listing={group.listing_cover_name}; listening={group.listening_cover_name}; "
            f"audioIds={','.join(str(item) for item in group.audio_source_ids)}; duration={group.duration_minutes} min"
        )
        if group.instrument_codes:
            lines.append(f"    instruments: {', '.join(group.instrument_codes)}")
    resolutions = list(contributor_resolutions)
    if resolutions:
        lines.append("  Contributors:")
        lines.extend(
            f"    - {item.display_name}: {'create' if item.contributor_id is None else f'reuse id={item.contributor_id}'}"
            for item in resolutions
        )
    if plan.warnings:
        lines.append("  Warnings:")
        lines.extend(f"    - {warning}" for warning in plan.warnings)
    return "\n".join(lines)


def remote_preflight(plan: LullabyPlan, client: TellPalAdminClient) -> tuple[ContributorResolution, ...]:
    existing = client.list_contents()
    conflicts = [
        item.get("externalKey")
        for item in existing
        if item.get("externalKey") in {group.external_key for group in plan.groups}
    ]
    if conflicts:
        raise LullabyRemotePreflightError(
            f"External key already exists; automatic update/resume is forbidden: {sorted(set(conflicts))}"
        )

    catalog = client.list_instrument_catalog("tr")
    catalog_codes = {str(item.get("code", "")).upper() for item in catalog}
    unknown_codes = sorted({code for group in plan.groups for code in group.instrument_codes if code not in catalog_codes})
    if unknown_codes:
        raise LullabyRemotePreflightError(f"Instrument catalog does not contain planned codes: {unknown_codes}")

    names = sorted({group.musician for group in plan.groups if group.musician})
    resolutions: list[ContributorResolution] = []
    for name in names:
        matches = _exact_contributor_matches(client.search_contributors(name), name)
        if len(matches) > 1:
            raise LullabyRemotePreflightError(
                f"Contributor name has multiple exact matches: {name!r}; ids={[item.get('contributorId') for item in matches]}"
            )
        contributor_id = _positive_int(matches[0].get("contributorId"), "contributorId") if matches else None
        resolutions.append(ContributorResolution(name, contributor_id))
    return tuple(resolutions)


def execute_import(
    plan: LullabyPlan,
    client: TellPalAdminClient,
    report: LullabyImportRunReport,
    resolutions: Iterable[ContributorResolution],
) -> dict[str, object]:
    assert_source_unchanged(plan)
    report.mark_running()
    uploaded: dict[str, int] = {}
    content_ids: list[int] = []
    ids_by_name = {item.display_name.casefold(): item.contributor_id for item in resolutions}

    for group in plan.groups:
        report.set_phase("create-content")
        content = client.create_content({
            "type": "LULLABY",
            "externalKey": group.external_key,
            "ageRange": None,
            "active": plan.active,
        })
        content_id = _positive_int(content.get("contentId"), "contentId")
        content_ids.append(content_id)
        report.add_content_id(content_id)
        report.record_step(f"content-created:{group.external_key}")

        listing_id = _upload_media(plan, client, report, group.listing_media_key, uploaded)
        listening_id = _upload_media(plan, client, report, group.listening_media_key, uploaded)
        audio_id = _upload_media(plan, client, report, group.audio_media_key, uploaded)

        report.set_phase("attach-covers")
        client.update_content(content_id, {
            "externalKey": group.external_key,
            "ageRange": None,
            "active": plan.active,
            "textlessCoverMediaId": None,
            "listeningCoverMediaId": listening_id,
            "listingCoverMediaId": listing_id,
        })
        report.record_step(f"covers-attached:{group.external_key}")

        report.set_phase("attach-playback")
        client.update_lullaby_playback(content_id, audio_id, group.duration_minutes)
        if group.instrument_codes:
            client.replace_lullaby_instruments(content_id, list(group.instrument_codes))
        report.record_step(f"playback-attached:{group.external_key}")

        report.set_phase("create-localizations")
        for localization in group.localizations:
            client.create_localization(content_id, localization.language_code, {
                "title": localization.title,
                "status": "DRAFT",
            })
        report.record_step(f"localizations-created:{group.external_key}")

        report.set_phase("assign-contributors")
        if group.musician:
            name_key = group.musician.casefold()
            contributor_id = ids_by_name.get(name_key)
            if contributor_id is None:
                matches = _exact_contributor_matches(client.search_contributors(group.musician), group.musician)
                if len(matches) > 1:
                    raise LullabyRemotePreflightError(f"Contributor became ambiguous during import: {group.musician!r}")
                if matches:
                    contributor_id = _positive_int(matches[0].get("contributorId"), "contributorId")
                else:
                    contributor_id = _positive_int(
                        client.create_contributor(group.musician, ["MUSICIAN"]).get("contributorId"),
                        "contributorId",
                    )
                ids_by_name[name_key] = contributor_id
            client.assign_contributor(content_id, {
                "contributorId": contributor_id,
                "role": "MUSICIAN",
                "languageCode": None,
                "creditName": None,
                "sortOrder": 0,
            })
        report.record_step(f"contributors-assigned:{group.external_key}")
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
        "languages": sorted({localization.language_code for group in plan.groups for localization in group.localizations}),
        "publishedLanguages": sorted({localization.language_code for group in plan.groups for localization in group.localizations}) if plan.publish else [],
        "mediaUploads": len(uploaded),
    }
    report.mark_success(summary)
    return summary


def verify_group(
    plan: LullabyPlan,
    group: LullabyGroupPlan,
    content_id: int,
    client: TellPalAdminClient,
    uploaded: dict[str, int],
    *,
    published: bool = False,
) -> None:
    content = client.get_content(content_id)
    if (
        content.get("type") != "LULLABY"
        or content.get("externalKey") != group.external_key
        or content.get("active") is not plan.active
    ):
        raise RuntimeError(f"Verification failed for {group.external_key}: content identity mismatch")
    if content.get("listingCoverMediaId") != uploaded[group.listing_media_key] or content.get("listeningCoverMediaId") != uploaded[group.listening_media_key]:
        raise RuntimeError(f"Verification failed for {group.external_key}: cover references mismatch")
    playback = content.get("playback") or {}
    if playback.get("audioMediaId") != uploaded[group.audio_media_key] or playback.get("durationMinutes") != group.duration_minutes:
        raise RuntimeError(f"Verification failed for {group.external_key}: playback mismatch")
    if playback.get("processingStatus") not in {"PENDING", "COMPLETED", None}:
        raise RuntimeError(f"Verification failed for {group.external_key}: unexpected processing state")
    localizations = {item.get("languageCode"): item for item in content.get("localizations", [])}
    expected_languages = {item.language_code for item in group.localizations}
    if set(localizations) != expected_languages:
        raise RuntimeError(f"Verification failed for {group.external_key}: localization languages mismatch")
    for localization in group.localizations:
        stored = localizations[localization.language_code]
        if stored.get("title") != localization.title or stored.get("audioMediaId") is not None or stored.get("coverMediaId") is not None:
            raise RuntimeError(f"Verification failed for {group.external_key}/{localization.language_code}: localization mismatch")
        expected_status = "PUBLISHED" if published else "DRAFT"
        if stored.get("status") != expected_status:
            raise RuntimeError(f"Verification failed for {group.external_key}/{localization.language_code}: status mismatch")
    for media_key in {group.listing_media_key, group.listening_media_key, group.audio_media_key}:
        media = plan.media_by_key(media_key)
        stored = client.get_media(uploaded[media_key])
        if stored.get("assetId") != uploaded[media_key] or stored.get("byteSize") != media.byte_size or stored.get("checksumSha256") != media.checksum_sha256:
            raise RuntimeError(f"Verification failed for media {media.source_name}: stored asset mismatch")
    instruments = client.list_lullaby_instruments(content_id)
    actual_codes = tuple(item.get("code") for item in instruments)
    if actual_codes != group.instrument_codes or any(
        item.get("displayOrder") != index for index, item in enumerate(instruments)
    ):
        raise RuntimeError(f"Verification failed for {group.external_key}: instrument selection mismatch")
    contributors = client.list_content_contributors(content_id)
    musicians = [
        item for item in contributors
        if item.get("role") == "MUSICIAN" and item.get("languageCode") is None
    ]
    if group.musician:
        if len(musicians) != 1 or str(musicians[0].get("contributorDisplayName", "")).casefold() != group.musician.casefold():
            raise RuntimeError(f"Verification failed for {group.external_key}: MUSICIAN contributor mismatch")
    elif musicians:
        raise RuntimeError(f"Verification failed for {group.external_key}: unexpected MUSICIAN contributor")


def _upload_media(plan: LullabyPlan, client: TellPalAdminClient, report: LullabyImportRunReport, media_key: str, uploaded: dict[str, int]) -> int:
    if media_key in uploaded:
        return uploaded[media_key]
    media = plan.media_by_key(media_key)
    response = client.upload_media(media.local_path, media.kind, media.checksum_sha256)
    asset_id = _positive_int(response.get("assetId"), "assetId")
    uploaded[media_key] = asset_id
    report.record_asset(asset_id=asset_id, media_key=media_key, source_name=media.source_name, kind=media.kind, checksum_sha256=media.checksum_sha256, byte_size=media.byte_size)
    return asset_id


def _exact_contributor_matches(items: Iterable[dict[str, object]], name: str) -> list[dict[str, object]]:
    expected = name.strip().casefold()
    return [item for item in items if str(item.get("displayName", "")).strip().casefold() == expected]


def _positive_int(value: object, field: str) -> int:
    if isinstance(value, bool) or not isinstance(value, int) or value <= 0:
        raise RuntimeError(f"API response field {field} must be a positive integer; received {value!r}")
    return value
