from __future__ import annotations

from story_import_models import ImportState
from story_manifest import StoryPlan
from tellpal_admin_client import TellPalAdminClient


class ImportVerificationError(RuntimeError):
    """Raised when stored API state differs from the approved story plan."""


def verify_pre_publication(
    plan: StoryPlan,
    client: TellPalAdminClient,
    state: ImportState,
) -> None:
    content_id = _content_id(state)
    content = client.get_content(content_id)
    _assert_equal(content.get("contentId"), content_id, "contentId")
    _assert_equal(content.get("type"), "STORY", "content.type")
    _assert_equal(content.get("externalKey"), plan.external_key, "content.externalKey")
    _assert_equal(content.get("active"), plan.active, "content.active")
    _assert_equal(content.get("ageRange"), plan.age_range, "content.ageRange")
    _assert_equal(content.get("pageCount"), plan.page_count, "content.pageCount")
    _assert_equal(content.get("textlessCoverMediaId"), state.textless_cover_id, "content.textlessCoverMediaId")
    _verify_localizations(
        plan,
        content,
        state,
        expected_published=False,
        allowed_processing_statuses={"PENDING"},
    )
    _verify_pages(plan, client.list_story_pages(content_id), state)
    _verify_contributors(client.list_content_contributors(content_id), state)
    _verify_assets(client, state)


def verify_post_publication(
    plan: StoryPlan,
    client: TellPalAdminClient,
    state: ImportState,
) -> dict[str, object]:
    content_id = _content_id(state)
    content = client.get_content(content_id)
    _verify_localizations(
        plan,
        content,
        state,
        expected_published=True,
        allowed_processing_statuses={"PENDING", "COMPLETED"},
    )
    pages = client.list_story_pages(content_id)
    contributors = client.list_content_contributors(content_id)
    _verify_pages(plan, pages, state)
    _verify_contributors(contributors, state)
    return {
        "contentId": content_id,
        "externalKey": plan.external_key,
        "localizationCount": len(plan.localizations),
        "pageCount": len(pages),
        "uploadedAssetCount": len(state.uploaded_assets),
        "contributorAssignmentCount": len(contributors),
        "publishedLanguages": [
            localization.language_code for localization in plan.localizations if localization.is_publish
        ],
    }


def _verify_localizations(
    plan: StoryPlan,
    content: dict[str, object],
    state: ImportState,
    *,
    expected_published: bool,
    allowed_processing_statuses: set[str],
) -> None:
    raw_localizations = content.get("localizations")
    if not isinstance(raw_localizations, list):
        raise ImportVerificationError("content.localizations is not a list")
    by_language = {
        item.get("languageCode"): item
        for item in raw_localizations
        if isinstance(item, dict) and isinstance(item.get("languageCode"), str)
    }
    _assert_equal(set(by_language), {item.language_code for item in plan.localizations}, "localization languages")
    for localization in plan.localizations:
        actual = by_language[localization.language_code]
        expected_status = "PUBLISHED" if expected_published and localization.is_publish else "DRAFT"
        _assert_equal(actual.get("title"), localization.title, f"{localization.language_code}.title")
        _assert_equal(actual.get("description"), localization.description, f"{localization.language_code}.description")
        _assert_equal(
            actual.get("durationMinutes"),
            localization.duration_minutes,
            f"{localization.language_code}.durationMinutes",
        )
        _assert_equal(
            actual.get("coverMediaId"),
            state.localized_cover_ids[localization.language_code],
            f"{localization.language_code}.coverMediaId",
        )
        _assert_equal(actual.get("status"), expected_status, f"{localization.language_code}.status")
        _assert_processing_status(
            actual.get("processingStatus"),
            allowed_processing_statuses,
            f"{localization.language_code}.processingStatus",
        )
        if expected_status == "PUBLISHED" and not actual.get("publishedAt"):
            raise ImportVerificationError(f"{localization.language_code}.publishedAt is missing")


def _verify_pages(plan: StoryPlan, pages: list[dict[str, object]], state: ImportState) -> None:
    _assert_equal(len(pages), plan.page_count, "story page count")
    by_number = {item.get("pageNumber"): item for item in pages}
    _assert_equal(set(by_number), set(range(1, plan.page_count + 1)), "story page numbers")
    localizations_by_language = {item.language_code: item for item in plan.localizations}
    for page_number in range(1, plan.page_count + 1):
        actual_page = by_number[page_number]
        _assert_equal(
            actual_page.get("textlessIllustrationMediaId"),
            state.textless_page_ids.get(page_number),
            f"page {page_number}.textlessIllustrationMediaId",
        )
        raw_localizations = actual_page.get("localizations")
        if not isinstance(raw_localizations, list):
            raise ImportVerificationError(f"page {page_number}.localizations is not a list")
        actual_by_language = {
            item.get("languageCode"): item
            for item in raw_localizations
            if isinstance(item, dict) and isinstance(item.get("languageCode"), str)
        }
        _assert_equal(set(actual_by_language), set(localizations_by_language), f"page {page_number} languages")
        for language_code, localization in localizations_by_language.items():
            actual = actual_by_language[language_code]
            expected_page = localization.pages[page_number - 1]
            illustration_id, audio_id = state.page_media_ids[(language_code, page_number)]
            _assert_equal(actual.get("bodyText"), expected_page.body_text, f"page {page_number} {language_code}.bodyText")
            _assert_equal(actual.get("illustrationMediaId"), illustration_id, f"page {page_number} {language_code}.illustration")
            _assert_equal(actual.get("audioMediaId"), audio_id, f"page {page_number} {language_code}.audio")


def _verify_contributors(assignments: list[dict[str, object]], state: ImportState) -> None:
    expected = {
        (
            item["contributorId"],
            item["role"],
            item["languageCode"],
            item["creditName"],
            item["sortOrder"],
        )
        for item in state.contributor_assignments
    }
    actual = {
        (
            item.get("contributorId"),
            item.get("role"),
            item.get("languageCode"),
            item.get("creditName"),
            item.get("sortOrder"),
        )
        for item in assignments
    }
    _assert_equal(actual, expected, "contributor assignments")


def _verify_assets(client: TellPalAdminClient, state: ImportState) -> None:
    for expected in state.uploaded_assets.values():
        actual = client.get_media(expected.asset_id)
        _assert_equal(actual.get("assetId"), expected.asset_id, f"asset {expected.asset_id}.assetId")
        _assert_equal(actual.get("kind"), expected.kind, f"asset {expected.asset_id}.kind")
        _assert_equal(
            actual.get("checksumSha256"),
            expected.checksum_sha256,
            f"asset {expected.asset_id}.checksumSha256",
        )
        _assert_equal(actual.get("byteSize"), expected.byte_size, f"asset {expected.asset_id}.byteSize")


def _content_id(state: ImportState) -> int:
    if state.content_id is None:
        raise ImportVerificationError("contentId is unavailable")
    return state.content_id


def _assert_equal(actual: object, expected: object, label: str) -> None:
    if actual != expected:
        raise ImportVerificationError(f"{label} mismatch: expected {expected!r}, received {actual!r}")


def _assert_processing_status(actual: object, allowed: set[str], label: str) -> None:
    if actual not in allowed:
        raise ImportVerificationError(
            f"{label} mismatch: expected one of {sorted(allowed)!r}, received {actual!r}"
        )
