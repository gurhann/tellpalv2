from __future__ import annotations

from dataclasses import dataclass
from typing import Iterable

from audio_story_import_report import AudioStoryImportRunReport, utc_now
from audio_story_manifest import AudioStoryPlan, AudioStoryRowPlan, assert_source_unchanged, write_status_csv
from tellpal_admin_client import AdminApiError, AdminTransportError, TellPalAdminClient


class AudioStoryRemotePreflightError(RuntimeError):
    """Raised when the authenticated read-only preflight cannot safely continue."""


class AudioStoryRowError(RuntimeError):
    """Raised for a deterministic row-level conflict during execution."""


@dataclass
class AudioStoryRemoteRow:
    row: AudioStoryRowPlan
    content: dict[str, object]
    localization: dict[str, object]
    existing_audio_media_id: int | None = None
    existing_cover_media_id: int | None = None


def format_preview(plan: AudioStoryPlan, *, api_base_url: str | None = None) -> str:
    counts = _status_counts(plan.rows)
    lines = [
        "Audio-story import preview",
        f"  CSV: {plan.csv_path}",
        f"  Status CSV: {plan.status_csv_path}",
        f"  Storage: {plan.storage_bucket}/{plan.cover_prefix} + /{plan.audio_prefix or '<root>'}",
        f"  Rows: {len(plan.rows)}; valid={counts['PENDING']}; localErrors={counts['ERROR']}; duplicates={counts['SKIPPED_DUPLICATE']}",
        f"  Media candidates: {len(plan.media)}",
    ]
    if api_base_url:
        lines.append(f"  API: {api_base_url}")
    for row in plan.rows:
        details = f"line={row.line_number} {row.language_code}/{row.legacy_id_text} {row.title!r} status={row.status}"
        if row.duration_minutes:
            details += f" duration={row.duration_minutes} min"
        if row.error:
            details += f" error={row.error}"
        lines.append(f"  - {details}")
    if plan.warnings:
        lines.append("  Warnings:")
        lines.extend(f"    - {warning}" for warning in plan.warnings)
    return "\n".join(lines)


def remote_preflight(plan: AudioStoryPlan, client: TellPalAdminClient) -> tuple[AudioStoryRemoteRow, ...]:
    existing = client.list_contents()
    contexts: list[AudioStoryRemoteRow] = []
    for row in plan.rows:
        if row.status != "PENDING":
            continue
        matches = _matching_localizations(existing, row)
        if not matches:
            _row_error(row, f"No exact STORY localization match for {row.language_code}/{row.title!r}")
            continue
        if len(matches) > 1:
            ids = sorted({item[0].get("contentId") for item in matches})
            _row_error(row, f"Multiple exact STORY localization matches for {row.language_code}/{row.title!r}: contentIds={ids}")
            continue
        content, localization = matches[0]
        context: AudioStoryRemoteRow | None = None
        try:
            content_id = _positive_int(content.get("contentId"), "contentId")
            row.content_id = content_id
            context = AudioStoryRemoteRow(row, content, localization)
            _inspect_existing_cover(context, client)
            _inspect_existing_narration(context, client)
        except AdminApiError as exception:
            if _is_fatal_preflight_error(exception):
                raise
            _row_error(row, str(exception))
        except AudioStoryRowError as exception:
            _row_error(row, str(exception))
        if context is not None:
            contexts.append(context)

    _reject_duplicate_targets(contexts)
    _reject_shared_cover_conflicts(contexts)
    return tuple(contexts)


def execute_import(
    plan: AudioStoryPlan,
    client: TellPalAdminClient,
    report: AudioStoryImportRunReport,
    contexts: Iterable[AudioStoryRemoteRow],
) -> dict[str, object]:
    assert_source_unchanged(plan)
    report.mark_running()
    uploaded: dict[str, int] = {}
    content_cover_ids: dict[int, int] = {}
    content_cover_checksums: dict[int, str] = {}
    context_list = list(contexts)
    for context in context_list:
        if context.existing_cover_media_id is not None:
            content_cover_ids[context.row.content_id or 0] = context.existing_cover_media_id
            content_cover_checksums[context.row.content_id or 0] = context.row.cover_checksum_sha256 or ""

    for context in context_list:
        row = context.row
        if row.status != "PENDING":
            report.record_row(row)
            continue
        try:
            content_id = row.content_id or _positive_int(context.content.get("contentId"), "contentId")
            row.content_id = content_id
            cached_checksum = content_cover_checksums.get(content_id)
            if cached_checksum and cached_checksum != row.cover_checksum_sha256:
                raise AudioStoryRowError("Different listening cover is already planned for this content")

            cover_id = content_cover_ids.get(content_id) or context.existing_cover_media_id
            if cover_id is None:
                report.set_phase("upload-cover")
                cover_id = _upload_media(plan, client, report, row.cover_media_key, uploaded)
                client.update_content(content_id, _content_update_body(context.content, cover_id))
                report.record_step(f"listening-cover-attached:{content_id}")
                content_cover_ids[content_id] = cover_id
                content_cover_checksums[content_id] = row.cover_checksum_sha256 or ""
            row.cover_asset_id = cover_id

            audio_id = context.existing_audio_media_id
            if audio_id is None:
                report.set_phase("upload-audio")
                audio_id = _upload_media(plan, client, report, row.audio_media_key, uploaded)
                report.set_phase("update-narration")
                client.update_localization(
                    content_id,
                    row.language_code,
                    _localization_update_body(context.localization, audio_id, row.duration_minutes),
                )
            row.audio_asset_id = audio_id

            _verify_row(plan, client, row, context, content_id, audio_id, cover_id)
            row.status = "SUCCESS"
            row.processed_at = utc_now()
            report.add_content_id(content_id)
            report.record_step(f"row-succeeded:{row.line_number}")
        except AdminTransportError:
            _mark_unknown_and_stop(row, report, plan)
            raise
        except AdminApiError as exception:
            if _is_ambiguous_mutation_error(exception):
                _mark_unknown_and_stop(row, report, plan)
                raise
            if _is_fatal_preflight_error(exception):
                raise
            _row_error(row, str(exception))
        except AudioStoryRowError as exception:
            _row_error(row, str(exception))
        except (RuntimeError, ValueError) as exception:
            _mark_unknown_and_stop(row, report, plan, reason=str(exception))
            raise
        finally:
            write_status_csv(plan)
            report.record_row(row)

    counts = _status_counts(plan.rows)
    summary = {
        "rows": len(plan.rows),
        "success": counts["SUCCESS"],
        "alreadyImported": counts["ALREADY_IMPORTED"],
        "duplicates": counts["SKIPPED_DUPLICATE"],
        "errors": counts["ERROR"],
        "unknown": counts["UNKNOWN"],
        "contentIds": sorted({row.content_id for row in plan.rows if row.content_id}),
        "mediaUploads": len(uploaded),
    }
    report.mark_success(summary)
    return summary


def _matching_localizations(
    contents: list[dict[str, object]], row: AudioStoryRowPlan
) -> list[tuple[dict[str, object], dict[str, object]]]:
    matches: list[tuple[dict[str, object], dict[str, object]]] = []
    for content in contents:
        if content.get("type") != "STORY":
            continue
        localizations = content.get("localizations")
        if not isinstance(localizations, list):
            continue
        for localization in localizations:
            if not isinstance(localization, dict):
                continue
            if localization.get("languageCode") != row.language_code:
                continue
            if _match_key(localization.get("title")) == row.match_key:
                matches.append((content, localization))
    return matches


def _inspect_existing_cover(context: AudioStoryRemoteRow, client: TellPalAdminClient) -> None:
    value = context.content.get("listeningCoverMediaId")
    if value is None:
        return
    cover_id = _positive_int(value, "listeningCoverMediaId")
    media = client.get_media(cover_id)
    _assert_media(media, cover_id, "IMAGE", context.row.cover_checksum_sha256)
    context.existing_cover_media_id = cover_id


def _inspect_existing_narration(context: AudioStoryRemoteRow, client: TellPalAdminClient) -> None:
    processing_status = context.localization.get("processingStatus")
    if processing_status not in {None, "PENDING", "COMPLETED"}:
        raise AudioStoryRowError(f"Existing localization has unsupported processing status {processing_status!r}")
    narration = context.localization.get("narration")
    if narration is None:
        return
    if not isinstance(narration, dict):
        raise AudioStoryRowError("Existing narration response is invalid")
    audio_id = _positive_int(narration.get("audioMediaId"), "narration.audioMediaId")
    duration = narration.get("durationMinutes")
    if duration != context.row.duration_minutes:
        raise AudioStoryRowError(
            f"Existing narration duration {duration!r} conflicts with source duration {context.row.duration_minutes!r}"
        )
    media = client.get_media(audio_id)
    _assert_media(media, audio_id, "AUDIO", context.row.audio_checksum_sha256)
    context.existing_audio_media_id = audio_id
    if context.existing_cover_media_id is not None:
        context.row.status = "ALREADY_IMPORTED"
        context.row.audio_asset_id = audio_id
        context.row.cover_asset_id = context.existing_cover_media_id
        context.row.processed_at = utc_now()


def _reject_shared_cover_conflicts(contexts: list[AudioStoryRemoteRow]) -> None:
    by_content: dict[int, list[AudioStoryRemoteRow]] = {}
    for context in contexts:
        if context.row.status == "PENDING" and context.row.content_id:
            by_content.setdefault(context.row.content_id, []).append(context)
    for content_id, items in by_content.items():
        checksums = {item.row.cover_checksum_sha256 for item in items}
        if len(checksums) > 1:
            for item in items:
                _row_error(item.row, f"Rows for content {content_id} specify different listening covers")


def _reject_duplicate_targets(contexts: list[AudioStoryRemoteRow]) -> None:
    seen: set[tuple[int, str]] = set()
    for context in contexts:
        if context.row.status != "PENDING" or not context.row.content_id:
            continue
        target = (context.row.content_id, context.row.language_code)
        if target in seen:
            _row_error(context.row, "Another non-identical CSV row targets the same localization")
        else:
            seen.add(target)


def _upload_media(
    plan: AudioStoryPlan,
    client: TellPalAdminClient,
    report: AudioStoryImportRunReport,
    media_key: str | None,
    uploaded: dict[str, int],
) -> int:
    if not media_key:
        raise AudioStoryRowError("Row has no validated media plan")
    if media_key in uploaded:
        return uploaded[media_key]
    media = plan.media_by_key(media_key)
    response = client.upload_media(media.local_path, media.kind, media.checksum_sha256)
    asset_id = _positive_int(response.get("assetId"), "assetId")
    expected_type = "IMAGE" if media.kind == "ORIGINAL_IMAGE" else "AUDIO"
    _assert_media(response, asset_id, expected_type, media.checksum_sha256)
    uploaded[media_key] = asset_id
    report.record_asset(
        assetId=asset_id,
        mediaKey=media_key,
        sourceName=media.source_name,
        kind=media.kind,
        checksumSha256=media.checksum_sha256,
        byteSize=media.byte_size,
    )
    return asset_id


def _content_update_body(content: dict[str, object], listening_cover_id: int) -> dict[str, object]:
    return {
        "externalKey": content.get("externalKey"),
        "ageRange": content.get("ageRange"),
        "active": content.get("active"),
        "textlessCoverMediaId": content.get("textlessCoverMediaId"),
        "listeningCoverMediaId": listening_cover_id,
        "listingCoverMediaId": content.get("listingCoverMediaId"),
    }


def _localization_update_body(
    localization: dict[str, object], audio_id: int, duration_minutes: int | None
) -> dict[str, object]:
    if duration_minutes is None or duration_minutes <= 0:
        raise AudioStoryRowError("Narration duration is missing or not positive")
    return {
        "title": localization.get("title"),
        "description": localization.get("description"),
        "bodyText": localization.get("bodyText"),
        "coverMediaId": localization.get("coverMediaId"),
        "audioMediaId": localization.get("audioMediaId"),
        "durationMinutes": localization.get("durationMinutes"),
        "status": localization.get("status"),
        "processingStatus": localization.get("processingStatus"),
        "publishedAt": localization.get("publishedAt"),
        "narration": {"audioMediaId": audio_id, "durationMinutes": duration_minutes},
    }


def _verify_row(
    plan: AudioStoryPlan,
    client: TellPalAdminClient,
    row: AudioStoryRowPlan,
    context: AudioStoryRemoteRow,
    content_id: int,
    audio_id: int,
    cover_id: int,
) -> None:
    content = client.get_content(content_id)
    if content.get("type") != "STORY" or content.get("listeningCoverMediaId") != cover_id:
        raise AudioStoryRowError(f"Verification failed for line {row.line_number}: content/listening cover mismatch")
    for field in ("externalKey", "ageRange", "active", "textlessCoverMediaId", "listingCoverMediaId"):
        if content.get(field) != context.content.get(field):
            raise AudioStoryRowError(f"Verification failed for line {row.line_number}: content field {field} changed")
    localizations = content.get("localizations")
    if not isinstance(localizations, list):
        raise AudioStoryRowError(f"Verification failed for line {row.line_number}: localization list missing")
    matches = [
        item for item in localizations
        if isinstance(item, dict) and item.get("languageCode") == row.language_code and _match_key(item.get("title")) == row.match_key
    ]
    if len(matches) != 1:
        raise AudioStoryRowError(f"Verification failed for line {row.line_number}: localization mismatch")
    narration = matches[0].get("narration")
    if not isinstance(narration, dict) or narration.get("audioMediaId") != audio_id or narration.get("durationMinutes") != row.duration_minutes:
        raise AudioStoryRowError(f"Verification failed for line {row.line_number}: narration mismatch")
    for field in (
        "title",
        "description",
        "bodyText",
        "coverMediaId",
        "audioMediaId",
        "durationMinutes",
        "status",
        "processingStatus",
        "publishedAt",
    ):
        if matches[0].get(field) != context.localization.get(field):
            raise AudioStoryRowError(f"Verification failed for line {row.line_number}: localization field {field} changed")
    if row.audio_media_key:
        media = plan.media_by_key(row.audio_media_key)
        _assert_media(client.get_media(audio_id), audio_id, "AUDIO", media.checksum_sha256)
    if row.cover_media_key and row.cover_asset_id:
        media = plan.media_by_key(row.cover_media_key)
        _assert_media(client.get_media(row.cover_asset_id), row.cover_asset_id, "IMAGE", media.checksum_sha256)


def _assert_media(media: dict[str, object], asset_id: int, expected_type: str, expected_checksum: str | None) -> None:
    if media.get("assetId") != asset_id:
        raise AudioStoryRowError(f"Asset response ID mismatch for {asset_id}")
    if media.get("mediaType") != expected_type:
        raise AudioStoryRowError(f"Asset {asset_id} is not {expected_type}")
    if expected_checksum and media.get("checksumSha256") != expected_checksum:
        raise AudioStoryRowError(f"Asset {asset_id} checksum conflicts with source")


def _is_fatal_preflight_error(exception: AdminApiError) -> bool:
    return exception.status in {401, 403} or exception.status >= 500


def _is_ambiguous_mutation_error(exception: AdminApiError) -> bool:
    return exception.method in {"POST", "PUT", "PATCH"} and (
        exception.status == 408 or exception.status == 429 or exception.status >= 500
    )


def _mark_unknown_and_stop(
    row: AudioStoryRowPlan,
    report: AudioStoryImportRunReport,
    plan: AudioStoryPlan,
    *,
    reason: str = "Mutation outcome is unknown; run stopped without retry",
) -> None:
    row.status = "UNKNOWN"
    row.error = reason
    row.processed_at = utc_now()
    report.record_row(row)
    write_status_csv(plan)


def _row_error(row: AudioStoryRowPlan, message: str) -> None:
    row.status = "ERROR"
    row.error = message
    row.processed_at = utc_now()


def _positive_int(value: object, field: str) -> int:
    if isinstance(value, bool) or not isinstance(value, int) or value <= 0:
        raise AudioStoryRowError(f"API response field {field} must be a positive integer; received {value!r}")
    return value


def _match_key(value: object) -> str:
    import re
    from story_manifest import normalize_text

    return re.sub(r"\s+", " ", normalize_text(value)).casefold()


def _status_counts(rows: Iterable[AudioStoryRowPlan]) -> dict[str, int]:
    counts = {"PENDING": 0, "SUCCESS": 0, "ALREADY_IMPORTED": 0, "SKIPPED_DUPLICATE": 0, "ERROR": 0, "UNKNOWN": 0}
    for row in rows:
        counts[row.status] = counts.get(row.status, 0) + 1
    return counts
