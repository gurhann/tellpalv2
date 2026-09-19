from __future__ import annotations

from dataclasses import dataclass
from pathlib import PurePosixPath
from typing import Iterable

from category_manifest import (
    CategoryGroupPlan,
    CategoryLocalizationPlan,
    CategoryPlan,
    CategoryValidationError,
    IMAGE_MIME_TYPES,
    assert_source_unchanged,
)
from tellpal_admin_client import AdminApiError, AdminTransportError, TellPalAdminClient


class CategoryImportError(RuntimeError):
    """Raised when remote category state cannot be safely reconciled."""


@dataclass(frozen=True)
class RemoteLocalizationPlan:
    source: CategoryLocalizationPlan
    action: str
    existing_image_media_id: int | None = None


@dataclass(frozen=True)
class RemoteGroupPlan:
    source: CategoryGroupPlan
    category_action: str
    category_id: int | None
    localizations: tuple[RemoteLocalizationPlan, ...]


def format_preview(plan: CategoryPlan, remote: Iterable[RemoteGroupPlan] | None = None) -> str:
    remote_by_slug = {item.source.slug: item for item in remote or ()}
    lines = [
        "TellPal category import preview",
        f"  CSV: {plan.csv_path}",
        f"  Source fingerprint: {plan.source_fingerprint}",
        f"  Rows: {plan.row_count}",
        f"  Groups: {len(plan.groups)}",
        f"  Languages: {', '.join(sorted({item.language_code for group in plan.groups for item in group.localizations}))}",
        f"  Unique image objects: {len(plan.image_keys)}",
        "  Policy: active=true, premium=false, localizations=DRAFT",
        "  Groups:",
    ]
    for group in plan.groups:
        state = remote_by_slug.get(group.slug)
        category_action = state.category_action if state else "CREATE"
        localization_actions = (
            ", ".join(f"{item.source.language_code}:{item.action}" for item in state.localizations)
            if state
            else ", ".join(f"{item.language_code}:CREATE" for item in group.localizations)
        )
        lines.append(
            f"    - {group.slug} [{group.canonical_type}] "
            f"category={category_action}; sourceIds={','.join(str(value) for value in group.source_ids)}; "
            f"localizations={localization_actions}"
        )
    return "\n".join(lines)


def remote_preflight(plan: CategoryPlan, client: TellPalAdminClient) -> tuple[RemoteGroupPlan, ...]:
    categories = client.list_categories()
    categories_by_slug: dict[str, dict[str, object]] = {}
    for category in categories:
        slug = _required_text(category.get("slug"), "category slug")
        if slug in categories_by_slug:
            raise CategoryImportError(f"Remote category list contains duplicate slug: {slug}")
        categories_by_slug[slug] = category

    result: list[RemoteGroupPlan] = []
    for group in plan.groups:
        existing = categories_by_slug.get(group.slug)
        if existing is None:
            category_action = "CREATE"
            category_id = None
            existing_localizations: dict[str, dict[str, object]] = {}
        else:
            category_id = _positive_int(existing.get("categoryId"), f"category {group.slug} id")
            category_action = "REUSE"
            _require_category_compatibility(group, existing)
            existing_localizations = _index_localizations(client.list_category_localizations(category_id), group.slug)
            expected_languages = {item.language_code for item in group.localizations}
            unexpected_languages = set(existing_localizations) - expected_languages
            if unexpected_languages:
                raise CategoryImportError(
                    f"Existing category {group.slug} contains unexpected localization languages "
                    f"{sorted(unexpected_languages)}; refusing mutation"
                )

        localizations: list[RemoteLocalizationPlan] = []
        for source in group.localizations:
            existing_localization = existing_localizations.get(source.language_code)
            if existing_localization is None:
                localizations.append(RemoteLocalizationPlan(source, "CREATE"))
                continue
            image_media_id = _positive_int_or_none(existing_localization.get("imageMediaId"))
            if image_media_id is None:
                raise CategoryImportError(
                    f"Group {group.slug}/{source.language_code} exists without an image asset; refusing overwrite"
                )
            _require_localization_compatibility(group.slug, source, existing_localization)
            asset = client.get_media(image_media_id)
            if not _is_compatible_image_asset(asset, source.image_object_path):
                raise CategoryImportError(
                    f"Group {group.slug}/{source.language_code} image asset conflicts with the approved source object"
                )
            localizations.append(RemoteLocalizationPlan(source, "REUSE", image_media_id))
        result.append(RemoteGroupPlan(group, category_action, category_id, tuple(localizations)))
    return tuple(result)


def execute_import(
    plan: CategoryPlan,
    client: TellPalAdminClient,
    report: object,
    remote: tuple[RemoteGroupPlan, ...],
) -> dict[str, object]:
    """Execute preflighted category mutations in group order and verify each group."""
    assert_source_unchanged(plan)
    _call_report(report, "mark_running")
    asset_ids_by_image_key: dict[str, int] = {}
    _seed_existing_asset_cache(remote, asset_ids_by_image_key)
    category_ids: list[int] = []
    created_localizations = 0
    reused_localizations = 0

    for group_plan in remote:
        try:
            image_ids = _resolve_group_images(group_plan, client, report, asset_ids_by_image_key)
            category_id = group_plan.category_id
            if category_id is None:
                response = client.create_category(
                    {
                        "slug": group_plan.source.slug,
                        "type": group_plan.source.canonical_type,
                        "premium": False,
                        "active": True,
                    }
                )
                category_id = _positive_int(response.get("categoryId"), f"created category {group_plan.source.slug} id")
                _call_report(report, "record_category", group_plan.source.slug, category_id, "CREATED")
            else:
                _call_report(report, "record_category", group_plan.source.slug, category_id, "REUSED")
            category_ids.append(category_id)

            for localization_plan in group_plan.localizations:
                source = localization_plan.source
                if localization_plan.action == "REUSE":
                    reused_localizations += 1
                    _call_report(
                        report,
                        "record_localization",
                        group_plan.source.slug,
                        source.source_id,
                        source.language_code,
                        "REUSED",
                        localization_plan.existing_image_media_id,
                    )
                    continue
                image_id = image_ids[source.image_key]
                client.create_category_localization(
                    category_id,
                    source.language_code,
                    {
                        "name": source.name,
                        "description": source.description,
                        "imageMediaId": image_id,
                        "status": "DRAFT",
                        "publishedAt": None,
                    },
                )
                created_localizations += 1
                _call_report(
                    report,
                    "record_localization",
                    group_plan.source.slug,
                    source.source_id,
                    source.language_code,
                    "CREATED",
                    image_id,
                )
            _verify_group(group_plan, category_id, client)
            _call_report(report, "record_step", f"verified:{group_plan.source.slug}")
        except (AdminApiError, AdminTransportError, CategoryValidationError, CategoryImportError, RuntimeError) as exception:
            _call_report(report, "mark_failure", exception)
            raise

    summary = {
        "groups": len(remote),
        "categoryIds": category_ids,
        "createdLocalizations": created_localizations,
        "reusedLocalizations": reused_localizations,
        "rows": plan.row_count,
    }
    _call_report(report, "mark_success", summary)
    return summary


def _resolve_group_images(
    group_plan: RemoteGroupPlan,
    client: TellPalAdminClient,
    report: object,
    cache: dict[str, int],
) -> dict[str, int]:
    image_ids: dict[str, int] = {}
    for localization in group_plan.localizations:
        source = localization.source
        if localization.action == "REUSE":
            asset_id = _positive_int(
                localization.existing_image_media_id,
                f"existing image for {group_plan.source.slug}/{source.language_code}",
            )
            cached_asset_id = cache.get(source.image_key)
            if cached_asset_id is not None and cached_asset_id != asset_id:
                raise CategoryImportError(
                    "The same source image maps to multiple existing assets"
                )
            cache[source.image_key] = asset_id
            image_ids[source.image_key] = asset_id
            continue
        if source.image_key in cache:
            image_ids[source.image_key] = cache[source.image_key]
            continue
        mime_type = _image_mime_type(source.image_object_path)
        response = client.register_media_asset(
            provider="FIREBASE_STORAGE",
            object_path=source.image_object_path,
            kind="ORIGINAL_IMAGE",
            mime_type=mime_type,
        )
        asset_id = _positive_int(response.get("assetId"), f"registered image for {source.source_id}")
        cache[source.image_key] = asset_id
        image_ids[source.image_key] = asset_id
        _call_report(report, "record_asset", source.image_key, asset_id, source.image_object_path)
    return image_ids


def _seed_existing_asset_cache(
    remote: tuple[RemoteGroupPlan, ...],
    cache: dict[str, int],
) -> None:
    for group_plan in remote:
        for localization in group_plan.localizations:
            if localization.action != "REUSE":
                continue
            source = localization.source
            asset_id = _positive_int(
                localization.existing_image_media_id,
                f"existing image for {group_plan.source.slug}/{source.language_code}",
            )
            previous_asset_id = cache.get(source.image_key)
            if previous_asset_id is not None and previous_asset_id != asset_id:
                raise CategoryImportError(
                    "The same source image maps to multiple existing assets"
                )
            cache[source.image_key] = asset_id


def _verify_group(group_plan: RemoteGroupPlan, category_id: int, client: TellPalAdminClient) -> None:
    localizations = _index_localizations(client.list_category_localizations(category_id), group_plan.source.slug)
    expected_languages = {item.language_code for item in group_plan.source.localizations}
    if set(localizations) != expected_languages:
        raise CategoryImportError(f"Verification failed for group {group_plan.source.slug}: language set mismatch")
    for source in group_plan.source.localizations:
        stored = localizations[source.language_code]
        image_media_id = _positive_int_or_none(stored.get("imageMediaId"))
        if (
            stored.get("name") != source.name
            or stored.get("description") != source.description
            or stored.get("status") != "DRAFT"
            or stored.get("publishedAt") is not None
            or image_media_id is None
        ):
            raise CategoryImportError(
                f"Verification failed for group {group_plan.source.slug}/{source.language_code}"
            )
        asset = client.get_media(image_media_id)
        if not _is_compatible_image_asset(asset, source.image_object_path):
            raise CategoryImportError(
                f"Verification failed for group {group_plan.source.slug}/{source.language_code} image asset"
            )


def _require_category_compatibility(group: CategoryGroupPlan, existing: dict[str, object]) -> None:
    if (
        existing.get("type") != group.canonical_type
        or existing.get("active") is not True
        or existing.get("premium") is not False
    ):
        raise CategoryImportError(
            f"Existing category {group.slug} conflicts with type or import policy; refusing overwrite"
        )


def _require_localization_compatibility(
    slug: str,
    source: CategoryLocalizationPlan,
    existing: dict[str, object],
) -> None:
    if (
        existing.get("name") != source.name
        or existing.get("description") != source.description
        or existing.get("status") != "DRAFT"
        or existing.get("publishedAt") is not None
    ):
        raise CategoryImportError(
            f"Existing localization {slug}/{source.language_code} conflicts with the source; refusing overwrite"
        )


def _index_localizations(items: list[dict[str, object]], slug: str) -> dict[str, dict[str, object]]:
    result: dict[str, dict[str, object]] = {}
    for item in items:
        language = _required_text(item.get("languageCode"), f"localization language for {slug}")
        if language in result:
            raise CategoryImportError(f"Remote category {slug} contains duplicate language: {language}")
        result[language] = item
    return result


def _image_mime_type(object_path: str) -> str:
    suffix = PurePosixPath(object_path).suffix.casefold()
    mime_type = IMAGE_MIME_TYPES.get(suffix)
    if mime_type is None:
        raise CategoryImportError(f"Unsupported category image extension: {suffix or '<none>'}")
    return mime_type


def _is_compatible_image_asset(asset: dict[str, object], object_path: str) -> bool:
    return (
        asset.get("provider") == "FIREBASE_STORAGE"
        and asset.get("objectPath") == object_path
        and asset.get("mediaType") == "IMAGE"
        and asset.get("kind") == "ORIGINAL_IMAGE"
    )


def _call_report(report: object, method: str, *arguments: object) -> None:
    callback = getattr(report, method, None)
    if callback is not None:
        callback(*arguments)


def _required_text(value: object, label: str) -> str:
    if not isinstance(value, str) or not value.strip():
        raise CategoryImportError(f"Remote {label} is missing")
    return value.strip()


def _positive_int(value: object, label: str) -> int:
    if isinstance(value, bool) or not isinstance(value, int) or value <= 0:
        raise CategoryImportError(f"Remote {label} must be a positive integer")
    return value


def _positive_int_or_none(value: object) -> int | None:
    if isinstance(value, bool) or not isinstance(value, int) or value <= 0:
        return None
    return value
