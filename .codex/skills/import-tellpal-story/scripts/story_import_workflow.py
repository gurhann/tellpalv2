from __future__ import annotations

from collections.abc import Iterable

from story_import_models import ContributorResolution, ImportState, UploadedAsset
from story_import_report import ImportRunReport
from story_import_verifier import verify_post_publication, verify_pre_publication
from story_manifest import StoryPlan, fingerprint_directory, normalize_key
from tellpal_admin_client import TellPalAdminClient


class RemotePreflightError(RuntimeError):
    """Raised after login but before the first content mutation."""


def remote_preflight(
    plan: StoryPlan,
    client: TellPalAdminClient,
    *,
    contributor_id_overrides: dict[str, int] | None = None,
) -> tuple[ContributorResolution, ...]:
    existing_contents = client.list_contents()
    matching_content_ids = [
        item.get("contentId")
        for item in existing_contents
        if item.get("externalKey") == plan.external_key
    ]
    if matching_content_ids:
        raise RemotePreflightError(
            f"External key already exists and automatic update/resume is forbidden: "
            f"{plan.external_key} (contentIds={matching_content_ids})"
        )

    resolutions: list[ContributorResolution] = []
    seen_names: set[str] = set()
    overrides = contributor_id_overrides or {}
    plan_name_keys = {normalize_key(item.display_name) for item in plan.contributor_assignments}
    unknown_override_names = sorted(set(overrides) - plan_name_keys)
    if unknown_override_names:
        raise RemotePreflightError(
            f"Contributor ID overrides reference names not used by this story: {unknown_override_names}"
        )
    for assignment in plan.contributor_assignments:
        name_key = normalize_key(assignment.display_name)
        if name_key in seen_names:
            continue
        seen_names.add(name_key)
        matches = exact_contributor_matches(client.search_contributors(assignment.display_name), assignment.display_name)
        selected_id = overrides.get(name_key)
        if selected_id is not None:
            matching_ids = [_positive_int(item.get("contributorId"), "contributorId") for item in matches]
            if selected_id not in matching_ids:
                raise RemotePreflightError(
                    f"Contributor override for {assignment.display_name!r} selected id={selected_id}, "
                    f"but exact-match ids are {matching_ids}"
                )
            resolutions.append(ContributorResolution(assignment.display_name, selected_id))
            continue
        if len(matches) > 1:
            ids = [item.get("contributorId") for item in matches]
            raise RemotePreflightError(
                f"Contributor name has multiple exact matches: {assignment.display_name!r}, ids={ids}"
            )
        contributor_id = _positive_int(matches[0].get("contributorId"), "contributorId") if matches else None
        resolutions.append(ContributorResolution(assignment.display_name, contributor_id))
    return tuple(resolutions)


def assert_source_unchanged(plan: StoryPlan) -> None:
    _, current_fingerprint = fingerprint_directory(plan.story_directory)
    if current_fingerprint != plan.fingerprint:
        raise RemotePreflightError(
            "Story folder changed after preview; rebuild the plan and review it again before importing"
        )


def execute_import(
    plan: StoryPlan,
    client: TellPalAdminClient,
    report: ImportRunReport,
    resolutions: Iterable[ContributorResolution],
) -> dict[str, object]:
    state = ImportState()
    report.mark_running()
    report.set_phase("create-content")
    content = client.create_content(
        {
            "type": "STORY",
            "externalKey": plan.external_key,
            "ageRange": plan.age_range,
            "active": plan.active,
        }
    )
    content_id = _positive_int(content.get("contentId"), "contentId")
    state.content_id = content_id
    report.set_content_id(content_id)
    report.record_step("content-created")

    _create_localizations(plan, client, report, state)
    _create_story_pages(plan, client, report, state)
    _create_page_localizations(plan, client, report, state)
    _attach_textless_media(plan, client, report, state)
    _assign_contributors(plan, client, report, state, resolutions)

    report.set_phase("verify-before-publication")
    verify_pre_publication(plan, client, state)
    report.record_step("pre-publication-state-verified")

    report.set_phase("publish-localizations")
    for localization in plan.localizations:
        if localization.is_publish:
            client.publish_localization(content_id, localization.language_code)
    report.record_step("requested-localizations-published")

    report.set_phase("verify-final-state")
    summary = verify_post_publication(plan, client, state)
    report.record_step("final-state-verified")
    report.mark_success(summary)
    return summary


def format_preview(
    plan: StoryPlan,
    *,
    api_base_url: str | None = None,
    contributor_resolutions: Iterable[ContributorResolution] | None = None,
) -> str:
    languages = ", ".join(localization.language_code for localization in plan.localizations)
    published = ", ".join(
        localization.language_code for localization in plan.localizations if localization.is_publish
    ) or "none"
    lines = [
        "TellPal story import preview",
        f"  Story directory: {plan.story_directory}",
        f"  API target: {api_base_url or '(local preflight only)'}",
        f"  External key: {plan.external_key}",
        f"  Age range: {plan.age_range}",
        f"  Active: {str(plan.active).lower()}",
        f"  Languages: {languages}",
        f"  Pages per language: {plan.page_count}",
        f"  Fingerprinted source files: {len(plan.fingerprinted_files)}",
        f"  Media uploads: {plan.expected_actions['media_uploads']}",
        f"  Content localizations: {plan.expected_actions['content_localizations']}",
        f"  Story pages: {plan.expected_actions['story_pages']}",
        f"  Page localizations: {plan.expected_actions['page_localizations']}",
        f"  Contributor assignments: {plan.expected_actions['contributor_assignments']}",
        f"  Publications: {plan.expected_actions['publications']}",
        "  Textless attachments: "
        + (
            f"{1 + len(plan.textless_page_paths)} (1 cover + {len(plan.textless_page_paths)} pages)"
            if plan.textless_cover_path is not None
            else "0 (explicitly skipped)"
        ),
        "  Fixed write calls excluding contributor creates: "
        f"{plan.expected_actions['fixed_mutations_excluding_contributor_creates']}",
        f"  Publish languages: {published}",
        f"  Source fingerprint: {plan.fingerprint}",
        "  Localizations:",
    ]
    lines.extend(
        "    - "
        f"{item.language_code}: title={item.title!r}, duration={item.duration_minutes}, "
        f"publish={str(item.is_publish).lower()}, cover={item.cover_path}, "
        f"pages/images/audio={len(item.pages)}/{len(item.pages)}/{len(item.pages)}, "
        f"description={item.description!r}"
        for item in plan.localizations
    )
    if contributor_resolutions is not None:
        resolution_list = list(contributor_resolutions)
        new_count = sum(item.contributor_id is None for item in resolution_list)
        fixed = plan.expected_actions["fixed_mutations_excluding_contributor_creates"]
        lines.extend(
            [
                f"  Existing contributors reused: {len(resolution_list) - new_count}",
                f"  Contributors to create: {new_count}",
                f"  Expected write calls: {fixed + new_count}",
                "  Contributor resolutions:",
            ]
        )
        lines.extend(
            f"    - {item.display_name}: "
            f"{'create' if item.contributor_id is None else f'reuse id={item.contributor_id}'}"
            for item in resolution_list
        )
    if plan.warnings:
        lines.append("  Warnings:")
        lines.extend(f"    - {warning}" for warning in plan.warnings)
    lines.append("  Contributor scopes:")
    lines.extend(
        f"    - {item.role}: {item.display_name} ({item.language_code or 'ALL_LANGUAGE'})"
        for item in plan.contributor_assignments
    )
    return "\n".join(lines)


def exact_contributor_matches(
    contributors: Iterable[dict[str, object]],
    display_name: str,
) -> list[dict[str, object]]:
    expected = normalize_key(display_name)
    return [item for item in contributors if normalize_key(item.get("displayName")) == expected]


def _create_localizations(
    plan: StoryPlan,
    client: TellPalAdminClient,
    report: ImportRunReport,
    state: ImportState,
) -> None:
    content_id = _content_id(state)
    report.set_phase("create-content-localizations")
    for localization in plan.localizations:
        cover_id = _upload_asset(plan, client, report, state, localization.cover_path, "ORIGINAL_IMAGE")
        state.localized_cover_ids[localization.language_code] = cover_id
        client.create_localization(
            content_id,
            localization.language_code,
            {
                "title": localization.title,
                "description": localization.description,
                "bodyText": None,
                "coverMediaId": cover_id,
                "audioMediaId": None,
                "durationMinutes": localization.duration_minutes,
                "status": "DRAFT",
                "processingStatus": "PENDING",
                "publishedAt": None,
            },
        )
    report.record_step("content-localizations-created")


def _create_story_pages(
    plan: StoryPlan,
    client: TellPalAdminClient,
    report: ImportRunReport,
    state: ImportState,
) -> None:
    content_id = _content_id(state)
    report.set_phase("create-story-pages")
    for expected_page_number in range(1, plan.page_count + 1):
        response = client.add_story_page(content_id)
        actual_page_number = _positive_int(response.get("pageNumber"), "pageNumber")
        if actual_page_number != expected_page_number:
            raise RuntimeError(
                f"Story page append returned page {actual_page_number}; expected {expected_page_number}"
            )
    report.record_step("story-pages-created")


def _create_page_localizations(
    plan: StoryPlan,
    client: TellPalAdminClient,
    report: ImportRunReport,
    state: ImportState,
) -> None:
    content_id = _content_id(state)
    report.set_phase("create-page-localizations")
    for localization in plan.localizations:
        for page in localization.pages:
            illustration_id = _upload_asset(
                plan,
                client,
                report,
                state,
                page.illustration_path,
                "ORIGINAL_IMAGE",
            )
            audio_id = _upload_asset(plan, client, report, state, page.audio_path, "ORIGINAL_AUDIO")
            state.page_media_ids[(localization.language_code, page.page_number)] = (
                illustration_id,
                audio_id,
            )
            client.upsert_story_page_localization(
                content_id,
                page.page_number,
                localization.language_code,
                {
                    "bodyText": page.body_text,
                    "audioMediaId": audio_id,
                    "illustrationMediaId": illustration_id,
                },
            )
    report.record_step("page-localizations-created")


def _attach_textless_media(
    plan: StoryPlan,
    client: TellPalAdminClient,
    report: ImportRunReport,
    state: ImportState,
) -> None:
    if plan.textless_cover_path is None:
        report.record_step("textless-media-skipped")
        return
    content_id = _content_id(state)
    report.set_phase("attach-textless-media")
    cover_id = _upload_asset(plan, client, report, state, plan.textless_cover_path, "ORIGINAL_IMAGE")
    state.textless_cover_id = cover_id
    client.update_content(
        content_id,
        {
            "externalKey": plan.external_key,
            "ageRange": plan.age_range,
            "active": plan.active,
            "textlessCoverMediaId": cover_id,
        },
    )
    for page_number, relative_path in enumerate(plan.textless_page_paths, start=1):
        media_id = _upload_asset(plan, client, report, state, relative_path, "ORIGINAL_IMAGE")
        state.textless_page_ids[page_number] = media_id
        client.update_story_page(content_id, page_number, media_id)
    report.record_step("textless-media-attached")


def _assign_contributors(
    plan: StoryPlan,
    client: TellPalAdminClient,
    report: ImportRunReport,
    state: ImportState,
    resolutions: Iterable[ContributorResolution],
) -> None:
    content_id = _content_id(state)
    report.set_phase("assign-contributors")
    ids_by_name = {normalize_key(item.display_name): item.contributor_id for item in resolutions}
    for assignment in plan.contributor_assignments:
        name_key = normalize_key(assignment.display_name)
        contributor_id = ids_by_name.get(name_key)
        if contributor_id is None:
            matches = exact_contributor_matches(
                client.search_contributors(assignment.display_name),
                assignment.display_name,
            )
            if len(matches) > 1:
                raise RemotePreflightError(
                    f"Contributor became ambiguous during import: {assignment.display_name!r}"
                )
            if matches:
                contributor_id = _positive_int(matches[0].get("contributorId"), "contributorId")
            else:
                created = client.create_contributor(assignment.display_name)
                contributor_id = _positive_int(created.get("contributorId"), "contributorId")
            ids_by_name[name_key] = contributor_id
        state.contributor_ids[name_key] = contributor_id
        request = {
            "contributorId": contributor_id,
            "role": assignment.role,
            "languageCode": assignment.language_code,
            "creditName": None,
            "sortOrder": 0,
        }
        client.assign_contributor(content_id, request)
        state.contributor_assignments.append(request)
    report.record_step("contributors-assigned")


def _upload_asset(
    plan: StoryPlan,
    client: TellPalAdminClient,
    report: ImportRunReport,
    state: ImportState,
    relative_path: str,
    kind: str,
) -> int:
    checksum = plan.checksum_for(relative_path)
    byte_size = plan.size_for(relative_path)
    response = client.upload_media(plan.resolve_path(relative_path), kind, checksum)
    asset_id = _positive_int(response.get("assetId"), "assetId")
    asset = UploadedAsset(asset_id, relative_path, kind, checksum, byte_size)
    state.uploaded_assets[asset_id] = asset
    report.record_asset(
        asset_id=asset_id,
        relative_path=relative_path,
        kind=kind,
        checksum_sha256=checksum,
        byte_size=byte_size,
    )
    return asset_id


def _content_id(state: ImportState) -> int:
    if state.content_id is None:
        raise RuntimeError("Content must exist before this import phase")
    return state.content_id


def _positive_int(value: object, field: str) -> int:
    if isinstance(value, bool) or not isinstance(value, int) or value <= 0:
        raise RuntimeError(f"API response field {field} must be a positive integer; received {value!r}")
    return value
