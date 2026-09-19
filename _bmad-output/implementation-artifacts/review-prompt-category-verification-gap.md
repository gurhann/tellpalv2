Read /Users/gurhankucuk/.codex/worktrees/b51d/tellpalv2/_bmad/render/bmad-build/tellpalv2-6615b93b5770/46e0230e3ddf92766382/review-prompts/verification-gap.md completely and follow it as your review instructions.

Review content:

diff --git a/.codex/skills/import-tellpal-story/SKILL.md b/.codex/skills/import-tellpal-story/SKILL.md
index b4c735c..610da31 100644
--- a/.codex/skills/import-tellpal-story/SKILL.md
+++ b/.codex/skills/import-tellpal-story/SKILL.md
@@ -18,6 +18,11 @@ documented in [references/audio-story-import.md](references/audio-story-import.m
 row to an existing STORY localization, map one root `{id}.zip` MP3 to its nested narration, attach a
 missing shared listening cover, and continue deterministic row failures with sidecar statuses.

+For the legacy public category CSV shape, use `inspect_categories.py` and `import_categories.py`
+documented in [references/category-import.md](references/category-import.md). They apply the reviewed
+33-group mapping, preserve `AUDIO_STORY` only as source provenance, register category images through
+the Admin API, and create/reuse category aggregates with DRAFT localizations.
+
 # Import TellPal Story

 Use the bundled scripts as the only mutation path. Do not recreate the API workflow manually and do not modify `be/`, `cms/src/`, migrations, or API contracts.
@@ -80,4 +85,3 @@ The importer logs in, performs read-only remote preflight, prints the approved p
 Return the content ID, external key, imported languages, page/media/contributor counts, published languages, and the absolute `result.json` path. On failure, report the failed phase and last endpoint from the run report and state that recovery is manual.

 Run artifacts are stored under `%LOCALAPPDATA%\TellPal\story-import-agent\runs\<run-id>\`. They are diagnostic only and cannot be used to resume.
-
diff --git a/.codex/skills/import-tellpal-story/scripts/tellpal_admin_client.py b/.codex/skills/import-tellpal-story/scripts/tellpal_admin_client.py
index 1bc06ac..a9b2eb1 100644
--- a/.codex/skills/import-tellpal-story/scripts/tellpal_admin_client.py
+++ b/.codex/skills/import-tellpal-story/scripts/tellpal_admin_client.py
@@ -79,6 +79,53 @@ class TellPalAdminClient:
     def list_contents(self) -> list[dict[str, object]]:
         return _expect_list(self._request_json("GET", "/api/admin/contents"), "content list")

+    def list_categories(self) -> list[dict[str, object]]:
+        return _expect_list(self._request_json("GET", "/api/admin/categories"), "category list")
+
+    def create_category(self, body: dict[str, object]) -> dict[str, object]:
+        return _expect_dict(
+            self._request_json("POST", "/api/admin/categories", body=body),
+            "created category",
+        )
+
+    def list_category_localizations(self, category_id: int) -> list[dict[str, object]]:
+        return _expect_list(
+            self._request_json("GET", f"/api/admin/categories/{category_id}/localizations"),
+            "category localization list",
+        )
+
+    def create_category_localization(
+        self,
+        category_id: int,
+        language_code: str,
+        body: dict[str, object],
+    ) -> dict[str, object]:
+        path = f"/api/admin/categories/{category_id}/localizations/{language_code}"
+        return _expect_dict(
+            self._request_json("POST", path, body=body),
+            "created category localization",
+        )
+
+    def register_media_asset(
+        self,
+        *,
+        provider: str,
+        object_path: str,
+        kind: str,
+        mime_type: str | None = None,
+    ) -> dict[str, object]:
+        body: dict[str, object] = {
+            "provider": provider,
+            "objectPath": object_path,
+            "kind": kind,
+        }
+        if mime_type:
+            body["mimeType"] = mime_type
+        return _expect_dict(
+            self._request_json("POST", "/api/admin/media", body=body),
+            "registered media asset",
+        )
+
     def get_content(self, content_id: int) -> dict[str, object]:
         return _expect_dict(self._request_json("GET", f"/api/admin/contents/{content_id}"), "content")



--- untracked file: .codex/skills/import-tellpal-story/references/category-import.md ---
# TellPal Category Import Contract

## Input and approved grouping

The input is `tellpal_public_categories.csv` with columns `id,name,description,language,type,image_url`.
The importer loads `category_import_mapping.json`; it never infers multilingual identity at runtime.
The approved mapping contains 33 slugs and accounts for all 105 source IDs exactly once.

The source language set is `tr`, `en`, `pt`, and `de`. Canonical category types are `STORY`,
`MEDITATION`, and `LULLABY`. Source `AUDIO_STORY` is accepted only for IDs 29, 31, 38, and 163;
these rows become the separate `audio-books` slug with canonical type `STORY`.

`image_url` is treated as a transient Firebase Storage locator. The manifest extracts only the
object path and stores a SHA-256 object-path key in reports. Query parameters, including signed
tokens, must never be printed or persisted.

## Local inspection

Run the no-write preflight first:

```text
python3 -B .codex/skills/import-tellpal-story/scripts/inspect_categories.py \
  /absolute/path/to/tellpal_public_categories.csv
```

The command reports the source fingerprint, row/group/language counts, source IDs, canonical types,
and image-key count. It rejects malformed headers, duplicate or unassigned IDs, unsupported values,
blank names, duplicate languages inside a group, unsafe Firebase object paths, and incompatible
mapping types. It does not perform HTTP requests.

## Live import

Set `TELLPAL_API_BASE_URL` and `TELLPAL_ADMIN_USERNAME`, then run the live command in an interactive
terminal. The script asks for the password without storing it and requires the standalone `import`
confirmation keyword. It performs remote read-only preflight before the prompt and repeats that
preflight after confirmation.

```text
python3 -B .codex/skills/import-tellpal-story/scripts/import_categories.py \
  /absolute/path/to/tellpal_public_categories.csv
```

New categories use `{type, premium:false, active:true}`. New localizations use
`{name, description, imageMediaId, status:"DRAFT", publishedAt:null}`. The importer registers each
unique Firebase object path once through `POST /api/admin/media`, then sends only the returned asset
ID to the category localization endpoint. Compatible existing categories and localizations are
reused; conflicting editorial state is not overwritten.

## Safety and recovery

No mutation occurs before local validation, remote preflight, confirmation, and a source fingerprint
recheck. A deterministic conflict blocks the affected group. An ambiguous transport error stops the
run and requires manual inspection; the importer never retries a mutation or deletes partial data.
Run artifacts are written outside the repository and source folder under the platform TellPal data
directory. They contain IDs, statuses, fingerprints, and object-path hashes only.

Category-content membership, display order, publication, and Audio Books narration-readiness curation
are intentionally deferred to a later operation.


--- untracked file: .codex/skills/import-tellpal-story/references/category_import_mapping.json ---
{
  "version": 1,
  "groups": [
    {"slug": "audio-books", "canonicalType": "STORY", "sourceIds": [29, 31, 38, 163]},
    {"slug": "lullaby-and-relaxing-music", "canonicalType": "LULLABY", "sourceIds": [14, 15, 45, 156]},
    {"slug": "meditation", "canonicalType": "MEDITATION", "sourceIds": [28, 32, 37, 162]},
    {"slug": "popular-favorites", "canonicalType": "STORY", "sourceIds": [1, 24, 84, 150]},
    {"slug": "family-and-friendship", "canonicalType": "STORY", "sourceIds": [13, 16, 88, 157]},
    {"slug": "empathy-and-tolerance", "canonicalType": "STORY", "sourceIds": [54, 55, 99, 169]},
    {"slug": "activities-and-fun", "canonicalType": "STORY", "sourceIds": [17, 18, 87, 158]},
    {"slug": "informative-stories", "canonicalType": "STORY", "sourceIds": [5, 19, 86, 153]},
    {"slug": "nature-and-discovery", "canonicalType": "STORY", "sourceIds": [10, 11, 89, 155]},
    {"slug": "sleeping-stories", "canonicalType": "STORY", "sourceIds": [22, 30, 82, 160]},
    {"slug": "serial-stories", "canonicalType": "STORY", "sourceIds": [34, 35, 81, 164]},
    {"slug": "earthworm-series", "canonicalType": "STORY", "sourceIds": [3, 60, 92, 152]},
    {"slug": "sharing-and-cooperation", "canonicalType": "STORY", "sourceIds": [50, 59, 98, 165]},
    {"slug": "emotional-awareness", "canonicalType": "STORY", "sourceIds": [51, 58, 94, 166]},
    {"slug": "problem-solving", "canonicalType": "STORY", "sourceIds": [52, 57, 100, 167]},
    {"slug": "confidence-and-courage", "canonicalType": "STORY", "sourceIds": [53, 56, 101, 168]},
    {"slug": "science", "canonicalType": "STORY", "sourceIds": [61, 62, 97, 170]},
    {"slug": "imagination-and-creativity", "canonicalType": "STORY", "sourceIds": [64, 66, 95, 171]},
    {"slug": "competition", "canonicalType": "STORY", "sourceIds": [65, 67, 96, 172]},
    {"slug": "magic-words", "canonicalType": "STORY", "sourceIds": [102, 103, 106, 173]},
    {"slug": "animated-stories", "canonicalType": "STORY", "sourceIds": [20, 21, 159]},
    {"slug": "stories-with-songs", "canonicalType": "STORY", "sourceIds": [27, 93, 161]},
    {"slug": "brand-new-stories", "canonicalType": "STORY", "sourceIds": [104, 105, 174]},
    {"slug": "animal-friends", "canonicalType": "STORY", "sourceIds": [2, 151]},
    {"slug": "parent-favorites", "canonicalType": "STORY", "sourceIds": [6, 154]},
    {"slug": "editors-pick", "canonicalType": "STORY", "sourceIds": [9, 90]},
    {"slug": "english-stories", "canonicalType": "STORY", "sourceIds": [7, 91]},
    {"slug": "adventure-stories", "canonicalType": "STORY", "sourceIds": [25, 83]},
    {"slug": "hello-summer", "canonicalType": "STORY", "sourceIds": [107, 175]},
    {"slug": "choice-stories", "canonicalType": "STORY", "sourceIds": [4]},
    {"slug": "special-occasions", "canonicalType": "STORY", "sourceIds": [12]},
    {"slug": "recommendations", "canonicalType": "STORY", "sourceIds": [26]},
    {"slug": "library-discovery", "canonicalType": "STORY", "sourceIds": [80]}
  ]
}


--- untracked file: .codex/skills/import-tellpal-story/scripts/category_import_report.py ---
from __future__ import annotations

import json
import os
import uuid
from datetime import UTC, datetime
from pathlib import Path
from typing import Any

from category_manifest import CategoryPlan


class CategoryImportRunReport:
    """Persists non-secret category import state for diagnosis and audit."""

    def __init__(self, plan: CategoryPlan, api_base_url: str, *, storage_root: Path | None = None):
        run_id = f"{datetime.now(UTC).strftime('%Y%m%dT%H%M%SZ')}-{uuid.uuid4().hex[:8]}"
        self.directory = (storage_root or run_storage_root()) / run_id
        self.directory.mkdir(parents=True, exist_ok=False)
        self.manifest_path = self.directory / "manifest.json"
        self.result_path = self.directory / "result.json"
        self.state: dict[str, Any] = {
            "runId": run_id,
            "createdAt": utc_now(),
            "status": "PREVIEW",
            "phase": "local-preflight",
            "apiBaseUrl": api_base_url,
            "sourceFingerprint": plan.source_fingerprint,
            "groupCount": len(plan.groups),
            "rowCount": plan.row_count,
            "categories": [],
            "localizations": [],
            "assets": [],
            "completedSteps": [],
            "lastRequest": None,
            "error": None,
        }
        atomic_write_json(self.manifest_path, plan.to_dict())
        self.flush()

    def mark_running(self) -> None:
        self.state["status"] = "RUNNING"
        self.state["phase"] = "mutating"
        self.state["startedAt"] = utc_now()
        self.flush()

    def record_category(self, slug: str, category_id: int, action: str) -> None:
        self.state["categories"].append({"slug": slug, "categoryId": category_id, "action": action})
        self.flush()

    def record_localization(
        self,
        slug: str,
        source_id: int,
        language_code: str,
        action: str,
        image_media_id: int | None,
    ) -> None:
        self.state["localizations"].append(
            {
                "slug": slug,
                "sourceId": source_id,
                "languageCode": language_code,
                "action": action,
                "imageMediaId": image_media_id,
            }
        )
        self.flush()

    def record_asset(self, image_key: str, asset_id: int, object_path: str) -> None:
        # image_key is already a SHA-256 of the object path; never persist the raw path.
        del object_path
        self.state["assets"].append({"imageKey": image_key, "assetId": asset_id})
        self.flush()

    def record_step(self, step: str) -> None:
        self.state["completedSteps"].append({"step": step, "at": utc_now()})
        self.flush()

    def record_last_request(self, request: dict[str, object] | None) -> None:
        self.state["lastRequest"] = request
        self.flush()

    def mark_cancelled(self) -> None:
        self.state["status"] = "CANCELLED"
        self.state["phase"] = "cancelled-before-write"
        self.state["cancelledAt"] = utc_now()
        self.flush()

    def mark_success(self, summary: dict[str, object]) -> None:
        self.state["status"] = "COMPLETED"
        self.state["phase"] = "complete"
        self.state["completedAt"] = utc_now()
        self.state["summary"] = summary
        self.flush()

    def mark_failure(self, exception: BaseException) -> None:
        self.state["status"] = "FAILED"
        self.state["failedAt"] = utc_now()
        self.state["error"] = {"type": type(exception).__name__, "message": str(exception)}
        self.flush()

    def flush(self) -> None:
        atomic_write_json(self.result_path, self.state)


def run_storage_root() -> Path:
    local_app_data = os.environ.get("LOCALAPPDATA")
    if local_app_data:
        return Path(local_app_data) / "TellPal" / "category-import-agent" / "runs"
    return Path.home() / ".local" / "share" / "TellPal" / "category-import-agent" / "runs"


def atomic_write_json(path: Path, payload: object) -> None:
    temporary = path.with_suffix(f"{path.suffix}.tmp")
    temporary.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    temporary.replace(path)


def utc_now() -> str:
    return datetime.now(UTC).isoformat()


--- untracked file: .codex/skills/import-tellpal-story/scripts/category_import_workflow.py ---
from __future__ import annotations

import mimetypes
from dataclasses import dataclass
from pathlib import PurePosixPath
from typing import Iterable

from category_manifest import (
    CategoryGroupPlan,
    CategoryLocalizationPlan,
    CategoryPlan,
    CategoryValidationError,
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
            if asset.get("objectPath") != source.image_object_path or asset.get("mediaType") != "IMAGE":
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
            image_ids[source.image_key] = _positive_int(
                localization.existing_image_media_id,
                f"existing image for {group_plan.source.slug}/{source.language_code}",
            )
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


def _verify_group(group_plan: RemoteGroupPlan, category_id: int, client: TellPalAdminClient) -> None:
    localizations = _index_localizations(client.list_category_localizations(category_id), group_plan.source.slug)
    expected_languages = {item.language_code for item in group_plan.source.localizations}
    if set(localizations) != expected_languages:
        raise CategoryImportError(f"Verification failed for group {group_plan.source.slug}: language set mismatch")
    for source in group_plan.source.localizations:
        stored = localizations[source.language_code]
        if (
            stored.get("name") != source.name
            or stored.get("description") != source.description
            or stored.get("status") != "DRAFT"
            or stored.get("publishedAt") is not None
            or _positive_int_or_none(stored.get("imageMediaId")) is None
        ):
            raise CategoryImportError(
                f"Verification failed for group {group_plan.source.slug}/{source.language_code}"
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
    mime_type = mimetypes.types_map.get(suffix)
    if mime_type not in {"image/jpeg", "image/png", "image/gif"}:
        raise CategoryImportError(f"Unsupported category image extension: {suffix or '<none>'}")
    return mime_type


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


--- untracked file: .codex/skills/import-tellpal-story/scripts/category_manifest.py ---
from __future__ import annotations

import csv
import hashlib
import json
import re
import unicodedata
from dataclasses import dataclass
from pathlib import Path
from urllib.parse import unquote, urlparse


SUPPORTED_LANGUAGES = {"tr", "en", "pt", "de"}
SUPPORTED_SOURCE_TYPES = {"STORY", "MEDITATION", "LULLABY", "AUDIO_STORY"}
CANONICAL_TYPES = {"STORY", "MEDITATION", "LULLABY"}
REQUIRED_COLUMNS = {"id", "name", "description", "language", "type", "image_url"}
DEFAULT_MAPPING_PATH = Path(__file__).resolve().parents[1] / "references" / "category_import_mapping.json"


class CategoryValidationError(ValueError):
    """Raised when the category CSV or its approved mapping is unsafe to import."""


@dataclass(frozen=True)
class CategoryLocalizationPlan:
    source_id: int
    line_number: int
    language_code: str
    name: str
    description: str | None
    source_type: str
    canonical_type: str
    image_object_path: str
    image_key: str


@dataclass(frozen=True)
class CategoryGroupPlan:
    slug: str
    canonical_type: str
    source_ids: tuple[int, ...]
    localizations: tuple[CategoryLocalizationPlan, ...]


@dataclass(frozen=True)
class CategoryPlan:
    csv_path: str
    mapping_path: str
    source_fingerprint: str
    groups: tuple[CategoryGroupPlan, ...]

    @property
    def row_count(self) -> int:
        return sum(len(group.localizations) for group in self.groups)

    @property
    def image_keys(self) -> tuple[str, ...]:
        return tuple(sorted({item.image_key for group in self.groups for item in group.localizations}))

    def to_dict(self) -> dict[str, object]:
        """Return a report-safe representation without signed URLs or raw storage paths."""
        return {
            "csvPath": self.csv_path,
            "mappingPath": self.mapping_path,
            "sourceFingerprint": self.source_fingerprint,
            "rowCount": self.row_count,
            "groupCount": len(self.groups),
            "imageCount": len(self.image_keys),
            "groups": [
                {
                    "slug": group.slug,
                    "canonicalType": group.canonical_type,
                    "sourceIds": list(group.source_ids),
                    "localizations": [
                        {
                            "sourceId": item.source_id,
                            "lineNumber": item.line_number,
                            "languageCode": item.language_code,
                            "name": item.name,
                            "description": item.description,
                            "sourceType": item.source_type,
                            "canonicalType": item.canonical_type,
                            "imageKey": item.image_key,
                        }
                        for item in group.localizations
                    ],
                }
                for group in self.groups
            ],
        }


def build_category_plan(
    csv_path: str | Path,
    *,
    mapping_path: str | Path | None = None,
) -> CategoryPlan:
    source = Path(csv_path).expanduser().resolve()
    if not source.is_file():
        raise CategoryValidationError(f"Category CSV does not exist: {source}")
    mapping = Path(mapping_path or DEFAULT_MAPPING_PATH).expanduser().resolve()
    groups = _load_mapping(mapping)
    rows = _read_rows(source)
    rows_by_id = {row["id"]: row for row in rows}
    expected_ids = [source_id for group in groups for source_id in group["source_ids"]]
    if len(expected_ids) != len(set(expected_ids)):
        raise CategoryValidationError("Approved category mapping contains duplicate source IDs")
    missing_ids = sorted(set(expected_ids) - set(rows_by_id))
    unexpected_ids = sorted(set(rows_by_id) - set(expected_ids))
    if missing_ids or unexpected_ids:
        raise CategoryValidationError(
            f"CSV and approved mapping differ; missing IDs={missing_ids}, unexpected IDs={unexpected_ids}"
        )

    planned_groups: list[CategoryGroupPlan] = []
    for group in groups:
        localizations: list[CategoryLocalizationPlan] = []
        seen_languages: set[str] = set()
        for source_id in group["source_ids"]:
            row = rows_by_id[source_id]
            source_type = row["type"]
            canonical_type = "STORY" if source_type == "AUDIO_STORY" else source_type
            if canonical_type != group["canonical_type"]:
                raise CategoryValidationError(
                    f"Line {row['line_number']}: source ID {source_id} maps to {group['slug']} "
                    f"with incompatible canonical type {canonical_type}"
                )
            if source_type == "AUDIO_STORY" and group["slug"] != "audio-books":
                raise CategoryValidationError(
                    f"Line {row['line_number']}: AUDIO_STORY source ID {source_id} must map to audio-books"
                )
            language = row["language"]
            if language in seen_languages:
                raise CategoryValidationError(
                    f"Group {group['slug']} has duplicate language localization: {language}"
                )
            seen_languages.add(language)
            object_path = parse_image_object_path(row["image_url"], row["line_number"])
            localizations.append(
                CategoryLocalizationPlan(
                    source_id=source_id,
                    line_number=row["line_number"],
                    language_code=language,
                    name=row["name"],
                    description=row["description"],
                    source_type=source_type,
                    canonical_type=canonical_type,
                    image_object_path=object_path,
                    image_key=hashlib.sha256(object_path.encode("utf-8")).hexdigest(),
                )
            )
        planned_groups.append(
            CategoryGroupPlan(
                slug=group["slug"],
                canonical_type=group["canonical_type"],
                source_ids=tuple(group["source_ids"]),
                localizations=tuple(localizations),
            )
        )
    return CategoryPlan(
        csv_path=str(source),
        mapping_path=str(mapping),
        source_fingerprint=sha256_file(source),
        groups=tuple(planned_groups),
    )


def assert_source_unchanged(plan: CategoryPlan) -> None:
    if sha256_file(plan.csv_path) != plan.source_fingerprint:
        raise CategoryValidationError("Category CSV changed after preview; rebuild the plan before importing")


def parse_image_object_path(image_url: str, line_number: int = 0) -> str:
    value = normalize_text(image_url)
    if len(value) >= 2 and value[0] == value[-1] == '"':
        value = value[1:-1].strip()
    parsed = urlparse(value)
    if parsed.scheme not in {"http", "https"} or not parsed.netloc:
        raise CategoryValidationError(_line_prefix(line_number) + "image_url must be an absolute HTTP(S) URL")
    match = re.search(r"/o/(.+)$", parsed.path)
    if not match:
        raise CategoryValidationError(_line_prefix(line_number) + "image_url does not contain a Firebase object path")
    object_path = unquote(match.group(1)).strip()
    if not object_path or object_path.startswith("/") or "\\" in object_path:
        raise CategoryValidationError(_line_prefix(line_number) + "Firebase object path is invalid")
    if any(part in {"", ".", ".."} for part in object_path.split("/")):
        raise CategoryValidationError(_line_prefix(line_number) + "Firebase object path contains an unsafe segment")
    return object_path


def normalize_text(value: object) -> str:
    return unicodedata.normalize("NFC", "" if value is None else str(value)).strip()


def normalize_optional_text(value: object) -> str | None:
    normalized = normalize_text(value)
    return normalized or None


def sha256_file(path: str | Path) -> str:
    digest = hashlib.sha256()
    with Path(path).open("rb") as source:
        for block in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def _read_rows(path: Path) -> list[dict[str, object]]:
    try:
        with path.open("r", encoding="utf-8-sig", newline="") as source:
            reader = csv.reader(source, strict=True)
            try:
                headers = [normalize_text(item) for item in next(reader)]
            except StopIteration as exception:
                raise CategoryValidationError("Category CSV is empty") from exception
            if len(headers) != len(set(headers)):
                raise CategoryValidationError("Category CSV has duplicate columns")
            missing = sorted(REQUIRED_COLUMNS - set(headers))
            unexpected = sorted(set(headers) - REQUIRED_COLUMNS)
            if missing or unexpected:
                raise CategoryValidationError(
                    f"Category CSV columns mismatch; missing={missing}, unexpected={unexpected}"
                )
            rows: list[dict[str, object]] = []
            seen_ids: set[int] = set()
            for line_number, values in enumerate(reader, start=2):
                if not values or all(not normalize_text(value) for value in values):
                    raise CategoryValidationError(f"Line {line_number}: blank row is not allowed")
                if len(values) != len(headers):
                    raise CategoryValidationError(f"Line {line_number}: column count does not match the header")
                raw = dict(zip(headers, values))
                try:
                    source_id = int(normalize_text(raw["id"]))
                except ValueError as exception:
                    raise CategoryValidationError(f"Line {line_number}: id must be a positive integer") from exception
                if source_id <= 0 or source_id in seen_ids:
                    raise CategoryValidationError(f"Line {line_number}: id must be unique and positive")
                language = normalize_text(raw["language"]).casefold()
                source_type = normalize_text(raw["type"]).upper()
                if language not in SUPPORTED_LANGUAGES:
                    raise CategoryValidationError(f"Line {line_number}: unsupported language {language!r}")
                if source_type not in SUPPORTED_SOURCE_TYPES:
                    raise CategoryValidationError(f"Line {line_number}: unsupported type {source_type!r}")
                name = normalize_text(raw["name"])
                if not name:
                    raise CategoryValidationError(f"Line {line_number}: name must not be blank")
                description = normalize_optional_text(raw["description"])
                image_url = normalize_text(raw["image_url"])
                if not image_url:
                    raise CategoryValidationError(f"Line {line_number}: image_url must not be blank")
                seen_ids.add(source_id)
                rows.append(
                    {
                        "id": source_id,
                        "line_number": line_number,
                        "name": name,
                        "description": description,
                        "language": language,
                        "type": source_type,
                        "image_url": image_url,
                    }
                )
            return rows
    except csv.Error as exception:
        raise CategoryValidationError(f"Category CSV is malformed: {exception}") from exception


def _load_mapping(path: Path) -> list[dict[str, object]]:
    if not path.is_file():
        raise CategoryValidationError(f"Category mapping does not exist: {path}")
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exception:
        raise CategoryValidationError(f"Category mapping cannot be read: {path}") from exception
    raw_groups = payload.get("groups") if isinstance(payload, dict) else None
    if not isinstance(raw_groups, list) or not raw_groups:
        raise CategoryValidationError("Category mapping must contain a non-empty groups list")
    result: list[dict[str, object]] = []
    seen_slugs: set[str] = set()
    for item in raw_groups:
        if not isinstance(item, dict):
            raise CategoryValidationError("Category mapping group must be an object")
        slug = normalize_text(item.get("slug"))
        canonical_type = normalize_text(item.get("canonicalType")).upper()
        source_ids = item.get("sourceIds")
        if not slug or slug in seen_slugs or not re.fullmatch(r"[a-z0-9]+(?:-[a-z0-9]+)*", slug):
            raise CategoryValidationError(f"Invalid or duplicate category slug in mapping: {slug!r}")
        if canonical_type not in CANONICAL_TYPES:
            raise CategoryValidationError(f"Unsupported canonical type in mapping: {canonical_type!r}")
        if not isinstance(source_ids, list) or not source_ids or any(not isinstance(value, int) for value in source_ids):
            raise CategoryValidationError(f"Category mapping group {slug} must have positive integer source IDs")
        if any(value <= 0 for value in source_ids):
            raise CategoryValidationError(f"Category mapping group {slug} contains a non-positive source ID")
        seen_slugs.add(slug)
        result.append({"slug": slug, "canonical_type": canonical_type, "source_ids": tuple(source_ids)})
    return result


def _line_prefix(line_number: int) -> str:
    return f"Line {line_number}: " if line_number else ""


--- untracked file: .codex/skills/import-tellpal-story/scripts/import_categories.py ---
from __future__ import annotations

import argparse
import getpass
import os
import sys

from category_import_report import CategoryImportRunReport
from category_import_workflow import (
    CategoryImportError,
    execute_import,
    format_preview,
    remote_preflight,
)
from category_manifest import CategoryValidationError, build_category_plan
from tellpal_admin_client import (
    AdminApiError,
    AdminTransportError,
    TellPalAdminClient,
    validate_base_url,
)


def main() -> int:
    _configure_console_encoding()
    parser = argparse.ArgumentParser(description="Import legacy TellPal categories through the Admin API.")
    parser.add_argument("csv_path", help="Path to tellpal_public_categories.csv")
    parser.add_argument("--mapping", help="Path to the approved category mapping JSON")
    parser.add_argument("--timeout-seconds", type=float, default=120)
    arguments = parser.parse_args()

    report: CategoryImportRunReport | None = None
    client: TellPalAdminClient | None = None
    try:
        if not sys.stdin.isatty():
            raise RuntimeError("Live category import requires an interactive terminal")
        api_base_url = validate_base_url(_required_environment("TELLPAL_API_BASE_URL"))
        username = _required_environment("TELLPAL_ADMIN_USERNAME")
        plan = build_category_plan(arguments.csv_path, mapping_path=arguments.mapping)
        report = CategoryImportRunReport(plan, api_base_url)
        client = TellPalAdminClient(api_base_url, timeout_seconds=arguments.timeout_seconds)
        password = getpass.getpass(f"TellPal admin password for {username}: ")
        if not password:
            raise RuntimeError("Admin password must not be empty")
        client.login(username, password)
        del password

        remote = remote_preflight(plan, client)
        print()
        print(format_preview(plan, remote))
        print()
        if input("Type 'import' to start writes: ") != "import":
            report.mark_cancelled()
            print(f"Import cancelled. Run report: {report.result_path}")
            return 1

        remote = remote_preflight(plan, client)
        summary = execute_import(plan, client, report, remote)
        print()
        print(
            f"Import completed: groups={summary['groups']}, "
            f"createdLocalizations={summary['createdLocalizations']}, "
            f"reusedLocalizations={summary['reusedLocalizations']}"
        )
        print(f"Run report: {report.result_path}")
        return 0
    except KeyboardInterrupt as exception:
        if report is not None:
            if client is not None:
                report.record_last_request(client.last_request)
            report.mark_failure(exception)
            print(f"Import interrupted. Run report: {report.result_path}", file=sys.stderr)
        return 130
    except (CategoryValidationError, CategoryImportError, AdminApiError, AdminTransportError, RuntimeError, OSError) as exception:
        if report is not None:
            if client is not None:
                report.record_last_request(client.last_request)
            report.mark_failure(exception)
            print(f"Run report: {report.result_path}", file=sys.stderr)
        print(f"Category import failed: {exception}", file=sys.stderr)
        return 2
    finally:
        if client is not None:
            try:
                client.logout()
            except Exception as exception:
                print(f"Warning: logout failed: {exception}", file=sys.stderr)


def _required_environment(name: str) -> str:
    value = os.environ.get(name, "").strip()
    if not value:
        raise RuntimeError(f"Required environment variable is missing: {name}")
    return value


def _configure_console_encoding() -> None:
    for stream in (sys.stdout, sys.stderr):
        reconfigure = getattr(stream, "reconfigure", None)
        if reconfigure:
            reconfigure(encoding="utf-8", errors="replace")


if __name__ == "__main__":
    raise SystemExit(main())


--- untracked file: .codex/skills/import-tellpal-story/scripts/inspect_categories.py ---
from __future__ import annotations

import argparse
import sys

from category_manifest import CategoryValidationError, build_category_plan
from category_import_workflow import format_preview


def main() -> int:
    _configure_console_encoding()
    parser = argparse.ArgumentParser(description="Inspect legacy TellPal categories without API writes.")
    parser.add_argument("csv_path", help="Path to tellpal_public_categories.csv")
    parser.add_argument("--mapping", help="Path to the approved category mapping JSON")
    arguments = parser.parse_args()
    try:
        plan = build_category_plan(arguments.csv_path, mapping_path=arguments.mapping)
        print(format_preview(plan))
        return 0
    except (CategoryValidationError, ValueError, OSError) as exception:
        print(f"Category preflight failed: {exception}", file=sys.stderr)
        return 2


def _configure_console_encoding() -> None:
    for stream in (sys.stdout, sys.stderr):
        reconfigure = getattr(stream, "reconfigure", None)
        if reconfigure:
            reconfigure(encoding="utf-8", errors="replace")


if __name__ == "__main__":
    raise SystemExit(main())


--- untracked file: .codex/skills/import-tellpal-story/scripts/tests/test_category_import.py ---
from __future__ import annotations

import json
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import MagicMock
from urllib.parse import quote

SCRIPT_DIR = Path(__file__).resolve().parents[1]
if str(SCRIPT_DIR) not in sys.path:
    sys.path.insert(0, str(SCRIPT_DIR))

from category_import_workflow import CategoryImportError, execute_import, remote_preflight
from category_manifest import CategoryValidationError, build_category_plan
from tellpal_admin_client import AdminApiError, TellPalAdminClient


class _Report:
    def __init__(self):
        self.assets = []
        self.categories = []
        self.localizations = []
        self.steps = []

    def mark_running(self):
        pass

    def record_asset(self, *arguments):
        self.assets.append(arguments)

    def record_category(self, *arguments):
        self.categories.append(arguments)

    def record_localization(self, *arguments):
        self.localizations.append(arguments)

    def record_step(self, step):
        self.steps.append(step)

    def mark_success(self, summary):
        self.summary = summary

    def mark_failure(self, exception):
        self.failure = exception


class _Client:
    def __init__(self):
        self.categories = {}
        self.localizations = {}
        self.assets = {}
        self.calls = []
        self.next_category_id = 100
        self.next_asset_id = 500

    def list_categories(self):
        return list(self.categories.values())

    def create_category(self, body):
        self.calls.append(("create_category", body))
        category = {
            "categoryId": self.next_category_id,
            "type": body["type"],
            "slug": body["slug"],
            "premium": body["premium"],
            "active": body["active"],
        }
        self.next_category_id += 1
        self.categories[body["slug"]] = category
        self.localizations[category["categoryId"]] = []
        return category

    def list_category_localizations(self, category_id):
        return list(self.localizations.get(category_id, []))

    def create_category_localization(self, category_id, language_code, body):
        self.calls.append(("create_localization", category_id, language_code, body))
        localization = {
            "categoryId": category_id,
            "languageCode": language_code,
            **body,
            "published": False,
        }
        self.localizations[category_id].append(localization)
        return localization

    def register_media_asset(self, *, provider, object_path, kind, mime_type):
        self.calls.append(("register_media", provider, object_path, kind, mime_type))
        asset = {
            "assetId": self.next_asset_id,
            "provider": provider,
            "objectPath": object_path,
            "mediaType": "IMAGE",
            "kind": kind,
            "mimeType": mime_type,
        }
        self.assets[self.next_asset_id] = asset
        self.next_asset_id += 1
        return asset

    def get_media(self, asset_id):
        return self.assets[asset_id]


class CategoryImportTest(unittest.TestCase):
    def test_approved_mapping_has_33_groups_and_105_source_ids(self):
        mapping_path = SCRIPT_DIR.parent / "references" / "category_import_mapping.json"
        payload = json.loads(mapping_path.read_text(encoding="utf-8"))
        groups = payload["groups"]
        source_ids = [source_id for group in groups for source_id in group["sourceIds"]]
        self.assertEqual(len(groups), 33)
        self.assertEqual(len(source_ids), 105)
        self.assertEqual(len(source_ids), len(set(source_ids)))

    def test_full_approved_plan_executes_33_groups_and_105_localizations(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            mapping = json.loads(
                (SCRIPT_DIR.parent / "references" / "category_import_mapping.json").read_text(encoding="utf-8")
            )
            rows = []
            languages = ("tr", "en", "pt", "de")
            for group in mapping["groups"]:
                source_type = "AUDIO_STORY" if group["slug"] == "audio-books" else group["canonicalType"]
                for index, source_id in enumerate(group["sourceIds"]):
                    rows.append(
                        (
                            source_id,
                            f"Category {source_id}",
                            languages[index],
                            source_type,
                            f"{group['slug']}.png",
                        )
                    )
            csv_path, mapping_path = _fixture_files(root, rows, mapping["groups"])
            plan = build_category_plan(csv_path, mapping_path=mapping_path)
            client = _Client()
            summary = execute_import(plan, client, _Report(), remote_preflight(plan, client))

        self.assertEqual(summary["groups"], 33)
        self.assertEqual(summary["createdLocalizations"], 105)
        self.assertEqual(len(client.categories), 33)
        self.assertEqual(sum(len(items) for items in client.localizations.values()), 105)

    def test_manifest_maps_audio_story_to_story_and_redacts_signed_url(self):
        with tempfile.TemporaryDirectory() as directory:
            csv_path, mapping_path = _fixture_files(Path(directory), [
                (29, "Sesli Kitaplar", "tr", "AUDIO_STORY", "image.png"),
                (2, "Animal Friends", "de", "STORY", "image.png"),
            ], [
                {"slug": "audio-books", "canonicalType": "STORY", "sourceIds": [29]},
                {"slug": "animal-friends", "canonicalType": "STORY", "sourceIds": [2]},
            ])
            plan = build_category_plan(csv_path, mapping_path=mapping_path)

        self.assertEqual(plan.groups[0].canonical_type, "STORY")
        self.assertEqual(plan.groups[0].localizations[0].source_type, "AUDIO_STORY")
        serialized = json.dumps(plan.to_dict(), ensure_ascii=False)
        self.assertNotIn("token=secret", serialized)
        self.assertNotIn("https://", serialized)

    def test_invalid_source_id_is_rejected_before_remote_work(self):
        with tempfile.TemporaryDirectory() as directory:
            csv_path, mapping_path = _fixture_files(Path(directory), [
                (1, "Stories", "tr", "STORY", "image.png"),
                (2, "Unexpected", "en", "STORY", "image.png"),
            ], [{"slug": "stories", "canonicalType": "STORY", "sourceIds": [1]}])
            with self.assertRaises(CategoryValidationError):
                build_category_plan(csv_path, mapping_path=mapping_path)

    def test_existing_type_conflict_does_not_mutate(self):
        plan = self._plan()
        client = _Client()
        client.categories["stories"] = {
            "categoryId": 7,
            "type": "MEDITATION",
            "slug": "stories",
            "premium": False,
            "active": True,
        }
        client.localizations[7] = []
        with self.assertRaises(CategoryImportError):
            remote_preflight(plan, client)
        self.assertEqual(client.calls, [])

    def test_image_registration_failure_happens_before_category_mutation(self):
        plan = self._plan()
        client = _Client()
        client.register_media_asset = MagicMock(
            side_effect=AdminApiError("POST", "/api/admin/media", 409, {"title": "conflict"})
        )
        report = _Report()
        remote = remote_preflight(plan, client)
        with self.assertRaises(AdminApiError):
            execute_import(plan, client, report, remote)
        self.assertEqual(client.categories, {})
        self.assertEqual(client.localizations, {})
        self.assertIsInstance(report.failure, AdminApiError)

    def test_rerun_reuses_categories_localizations_and_assets(self):
        plan = self._plan()
        client = _Client()
        first_report = _Report()
        execute_import(plan, client, first_report, remote_preflight(plan, client))
        first_call_count = len(client.calls)

        second_report = _Report()
        second_remote = remote_preflight(plan, client)
        self.assertEqual(second_remote[0].category_action, "REUSE")
        self.assertTrue(all(item.action == "REUSE" for item in second_remote[0].localizations))
        execute_import(plan, client, second_report, second_remote)

        self.assertEqual(len(client.categories), 1)
        self.assertEqual(len(client.localizations[100]), 2)
        self.assertEqual(len(client.assets), 1)
        self.assertEqual(len(client.calls), first_call_count)

    def test_admin_client_exposes_category_and_media_contracts(self):
        client = TellPalAdminClient("https://api.test")
        client._request_json = MagicMock(side_effect=[[], {"categoryId": 7}, [], {"assetId": 9}])
        self.assertEqual(client.list_categories(), [])
        self.assertEqual(client.create_category({"slug": "stories"})["categoryId"], 7)
        self.assertEqual(client.list_category_localizations(7), [])
        self.assertEqual(client.register_media_asset(
            provider="FIREBASE_STORAGE",
            object_path="category_images/image.png",
            kind="ORIGINAL_IMAGE",
            mime_type="image/png",
        )["assetId"], 9)
        calls = client._request_json.call_args_list
        self.assertEqual(calls[0].args[:2], ("GET", "/api/admin/categories"))
        self.assertEqual(calls[1].args[:2], ("POST", "/api/admin/categories"))
        self.assertEqual(calls[2].args[:2], ("GET", "/api/admin/categories/7/localizations"))
        self.assertEqual(calls[3].args[:2], ("POST", "/api/admin/media"))

    def _plan(self):
        with tempfile.TemporaryDirectory() as directory:
            csv_path, mapping_path = _fixture_files(Path(directory), [
                (1, "Stories", "tr", "STORY", "image.png"),
                (2, "Stories", "en", "STORY", "image.png"),
            ], [{"slug": "stories", "canonicalType": "STORY", "sourceIds": [1, 2]}])
            # The plan keeps the source path and fingerprint, so copy the fixture to a stable temp file.
            stable_directory = Path(tempfile.mkdtemp(prefix="tellpal-category-plan-"))
            stable_csv = stable_directory / "categories.csv"
            stable_mapping = stable_directory / "mapping.json"
            stable_csv.write_bytes(Path(csv_path).read_bytes())
            stable_mapping.write_bytes(Path(mapping_path).read_bytes())
        return build_category_plan(stable_csv, mapping_path=stable_mapping)


def _fixture_files(directory: Path, rows, groups):
    csv_path = directory / "categories.csv"
    csv_path.write_text(
        "id,name,description,language,type,image_url\n"
        + "\n".join(
            f'{source_id},{name},Description,{language},{source_type},'
            f'"https://firebasestorage.googleapis.com/v0/b/test/o/category_images%2F{quote(image)}?alt=media&token=secret"'
            for source_id, name, language, source_type, image in rows
        )
        + "\n",
        encoding="utf-8",
    )
    mapping_path = directory / "mapping.json"
    mapping_path.write_text(json.dumps({"version": 1, "groups": groups}), encoding="utf-8")
    return csv_path, mapping_path


if __name__ == "__main__":
    unittest.main()


--- untracked file: _bmad-output/implementation-artifacts/spec-category-import.md ---
---
title: 'Import legacy multilingual categories through the Admin API'
type: 'feature'
created: '2026-09-19'
status: 'in-review'
baseline_commit: 'eb54da0237f5c4f705c05e0646f6e33225d90a44'
review_loop_iteration: 0
context:
  - `{project-root}/AGENTS.md`
  - `{project-root}/be/docs/project-memory.md`
  - `{project-root}/architecture.md`
  - `{project-root}/be/docs/adr/ADR-0012-remove-audio-story-canonical-type.md`
  - `{project-root}/.codex/skills/import-tellpal-story/references/admin-api.md`
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** `tellpal_public_categories.csv` contains 105 multilingual legacy rows without stable group keys or slugs, and four `AUDIO_STORY` rows that cannot be persisted as a canonical v2 category type. The current system needs one category aggregate per approved editorial group and one language-scoped localization per source row.

**Approach:** Add a guarded category manifest, inspection command, and live importer beside the existing TellPal import scripts. Use the approved 33-group mapping explicitly, validate the complete CSV before writes, register Firebase image object paths through the Admin API, create compatible category/localization records idempotently, and persist a non-secret audit report.

## Boundaries & Constraints

**Always:** Preserve the CSV and source IDs; assign all 105 rows exactly once to the approved 33 groups. Use `STORY`, `MEDITATION`, and `LULLABY` only. Map `AUDIO_STORY` rows 29/31/38/163 to the separate `audio-books` slug with canonical type `STORY`; retain the source type only in the audit manifest. Trim names/descriptions, accept `tr`, `en`, `pt`, and `de`, and import new categories as `active=true`, `premium=false` with all localizations `DRAFT` and `publishedAt=null`. Derive Firebase object paths from image URLs without logging query strings or tokens. Resolve/register each unique image object once per run and retain only asset IDs/object-path fingerprints in reports. Perform local validation and read-only remote preflight before the exact `import` confirmation; recheck the CSV fingerprint before the first mutation. Compatible existing slugs/localizations are reused; type, policy, language, or content conflicts are reported without overwriting editorial data. Category-content membership and Audio Books audio-readiness curation are out of this import.

**Ask First:** None; the operator has accepted `DRAFT`, `active=true`, `premium=false`, the 33-group mapping, and category/localization-only scope.

**Never:** Create `AUDIO_STORY` category rows, infer groups from image URLs or translated names at runtime, persist signed Firebase URLs, mutate the source CSV, publish localizations, attach content, or retry an ambiguous mutation response.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|---------------|----------------------------|----------------|
| HAPPY_PATH | Valid CSV, approved mapping, resolvable images | 33 categories and 105 DRAFT localizations are created/reused | Report all source-to-v2 mappings |
| INVALID_SOURCE | Missing columns, duplicate/unknown ID, unsupported language/type, blank text, unassigned ID | No remote mutation | Deterministic validation error with line number |
| AUDIO_STORY | Source type `AUDIO_STORY` | One `audio-books` STORY category with four localizations | Reject any canonical AUDIO_STORY payload |
| EXISTING_STATE | Compatible or conflicting slug/localization | Compatible records are skipped; conflicts remain unchanged | Record conflict and stop the affected group |
| IMAGE_FAILURE | Invalid Firebase URL, missing object path, or registration conflict without a known asset ID | Localization is not mutated | Record unresolved image work without exposing the URL token |
| RERUN | Same CSV and existing imported records | No duplicate category or category-language rows | Verify counts and source fingerprint |

</frozen-after-approval>

## Code Map

- `{project-root}/_bmad-output/specs/spec-category-import/SPEC.md` and `category-import-mapping.md` -- approved 105-row source profile, 33 slugs, Audio Books rule, policy decisions, and data-quality findings.
- `{project-root}/.codex/skills/import-tellpal-story/scripts/tellpal_admin_client.py:34-241` -- authenticated JSON client and multipart media path; extend with category, localization, and storage-location registration calls.
- `{project-root}/be/src/main/java/com/tellpal/v2/category/web/admin/CategoryAdminController.java:42-182` -- existing category list/create/read and localization list/create/update endpoints; no bulk endpoint exists.
- `{project-root}/be/src/main/java/com/tellpal/v2/category/application/CategoryManagementService.java:25-115` and `category/domain/Category.java:32-132` -- slug uniqueness, image reference validation, localization upsert, and DRAFT/PUBLISHED invariants.
- `{project-root}/be/src/main/java/com/tellpal/v2/category/domain/CategoryType.java:7-21` and `{project-root}/be/docs/adr/ADR-0012-remove-audio-story-canonical-type.md:16-34` -- canonical type boundary and Audio Books compatibility rule.
- `{project-root}/be/src/main/java/com/tellpal/v2/asset/web/admin/AssetAdminController.java:87-183` and `asset/application/AssetRegistryService.java:194-249` -- backend upload/registration and storage-location identity; signed URLs must remain transient.
- `{project-root}/yuklenecek_hikayeler/tellpal_public_categories.csv` -- 105-row UTF-8 source supplied by the operator; do not copy or rewrite it.

## Tasks & Acceptance

**Execution:**
- [x] `.codex/skills/import-tellpal-story/references/category-import.md` and `category_import_mapping.json` -- document the approved mapping, policies, payloads, report format, and recovery rules.
- [x] `.codex/skills/import-tellpal-story/scripts/category_manifest.py` -- parse/normalize/validate the CSV, enforce the explicit 33-group mapping, derive safe object paths, and produce a fingerprinted plan without tokens.
- [x] `.codex/skills/import-tellpal-story/scripts/tellpal_admin_client.py` -- add category/localization reads and writes plus media registration with the existing authentication/retry rules.
- [x] `.codex/skills/import-tellpal-story/scripts/category_import_workflow.py`, `category_import_report.py`, `inspect_categories.py`, and `import_categories.py` -- implement local inspection, read-only remote preflight, confirmation gate, idempotent group execution, verification, and atomic non-secret reporting.
- [x] `.codex/skills/import-tellpal-story/scripts/tests/test_category_*.py` and `SKILL.md` -- cover source validation, grouping, Audio Books mapping, token redaction, conflicts, reruns, and CLI usage.

**Acceptance Criteria:**
- Given the supplied CSV, local inspection reports 105 rows, 33 groups, 4 languages, and no unassigned source IDs without making HTTP requests.
- Given a confirmed valid plan, the importer creates or reuses 33 canonical categories and 105 DRAFT localizations; repeated execution creates no duplicate slug or category-language row.
- Given Audio Books rows, the resulting category slug is `audio-books`, its type is `STORY`, and no `AUDIO_STORY` value is sent to the API.
- Given a malformed source, group conflict, unresolved image, or changed fingerprint, the importer performs no affected mutation and emits an actionable, token-free diagnostic.

## Spec Change Log

## Design Notes

The current API separates category and localization mutations, so the workflow preflights the complete group and records partial/unknown outcomes explicitly. The approved mapping is data, not a runtime heuristic: translations with missing language peers remain intentional single/partial groups, while reused image paths are deduplicated only as asset identity and never treated as editorial grouping evidence.

## Verification

**Commands:**
- `python3 -B .codex/skills/import-tellpal-story/scripts/inspect_categories.py /Users/gurhankucuk/Documents/GitHub/tellpalv2/yuklenecek_hikayeler/tellpal_public_categories.csv` -- expected: deterministic 105-row/33-group preview with no HTTP writes.
- `python3 -B -m unittest discover -s .codex/skills/import-tellpal-story/scripts/tests -p 'test_category_*.py'` -- expected: all category importer tests pass.
- `python3 -B -m unittest discover -s .codex/skills/import-tellpal-story/scripts/tests -p 'test_*.py'` -- expected: existing and category importer tests pass, with unrelated baseline failures reported separately.


--- untracked file: _bmad-output/specs/spec-category-import/.memlog.md ---
---
topic: TellPal public category import
goal: Import legacy multilingual public categories into the v2 category model without losing grouping or audio-story discovery semantics
updated: 2026-09-19T12:24
---

- (direction) User direction: preserve AUDIO_STORY source rows as a separately listed Audio Books category; do not discard them merely because AUDIO_STORY is no longer a canonical type.
- (decision) The Audio Books category remains a distinct category identity with its own slug and localizations, while its canonical category type remains STORY to comply with ADR-0012.
- (note) The source CSV is /Users/gurhankucuk/Documents/GitHub/tellpalv2/yuklenecek_hikayeler/tellpal_public_categories.csv with 105 rows, four supported languages (tr, en, pt, de), and 105 unique source ids.
- (capability) CAP-1 intent: The importer can validate the category CSV before mutation; success: schema, encoding, required text, supported languages, source-id uniqueness, and source-type counts are reported deterministically in a dry run.
- (capability) CAP-2 intent: The importer can map source rows into explicit category groups and language localizations; success: each source row is assigned exactly once to a reviewed group or an explicit unresolved bucket, and no group is inferred solely from an image URL.
- (capability) CAP-3 intent: The importer can create or reconcile category aggregates and localizations idempotently; success: a rerun does not create duplicate slugs or duplicate category-language localizations, and the source-id mapping remains available for audit.
- (capability) CAP-4 intent: The importer can preserve the Audio Books discovery category without persisting AUDIO_STORY as a canonical category type; success: source ids 29, 31, 38, and 163 become one distinct audio-books category with STORY canonical compatibility and four localizations.
- (capability) CAP-5 intent: The importer can resolve category images into v2 media references or report unresolved image work; success: category localizations receive valid imageMediaId values only after backend-mediated asset registration, and source download tokens are not persisted.
- (constraint) Canonical category types are STORY, MEDITATION, and LULLABY; AUDIO_STORY is a source presentation/discovery label and cannot be sent to the current category create/update contract.
- (constraint) Category grouping is represented by one aggregate and one stable slug with language-scoped localizations; the current model has no parent, family, or group-id field.
- (constraint) Category curation accepts content whose canonical content type matches the category type; the CSV contains no category-content links or display orders, so curation is a separate phase.
- (constraint) Audio Books curation must select STORY localizations that are audio-ready; the current validator checks STORY type, active state, and published localization but does not itself enforce narration readiness.
- (constraint) Category localization image_url values are source references only; v2 stores positive imageMediaId values and asset uploads are backend-mediated.
- (constraint) The source includes no slug, premium, active, status, or publishedAt values; import policy must supply or explicitly review these values.
- (decision) The provisional source mapping contains 33 candidate category aggregates and 105 localizations; this count is not final until ambiguous editorial clusters and image anomalies are reviewed.
- (assumption) The current request scopes this work to category aggregates and localizations; category-content curation and ordering will be handled after content ids and audio readiness are known.
- (assumption) The source Firebase image URLs may be used transiently to obtain assets, but their token-bearing URLs will not be committed, persisted, or emitted in reports.
- (question) Should imported category localizations start as DRAFT, or should the importer publish them with a supplied publication timestamp?
- (question) What are the intended default values for premium and active for this public-category import?
- (question) Are the provisional 33 group mappings accepted, especially Favorites, Parent Favorites, Editors' Pick, Sharing/Solidarity, Imagination/Creativity, and the single-language categories?
- (question) Should the importer only create the Audio Books category now, or also validate and attach audio-ready STORY content in the same rollout?
- (event) Activation complete: customization and project config resolved; no activation prepend or append steps were configured; the configured project-context.md persistent fact was missing and was not fabricated.
- (event) Self-validation coherence passed: five capabilities include intent and testable success; canonical type constraints, Audio Books behavior, explicit non-goals, assumptions, and unresolved policy questions are represented.
- (event) Self-validation preservation passed: the source profile, target-field mapping, 33 candidate groups, all 105 source IDs exactly once, Audio Books direction, image anomalies, whitespace anomalies, and curation boundary are present in the companion.
- (event) Spec finalized for review: downstream implementation must resolve publication state, premium/active defaults, provisional group approvals, and Audio Books curation scope before mutation.


--- untracked file: _bmad-output/specs/spec-category-import/SPEC.md ---
---
id: SPEC-category-import
companions:
  - category-import-mapping.md
  - ../spec-story-audio-experience/SPEC.md
  - ../spec-story-audio-experience/audio-story-contract.md
  - ../../../architecture.md
  - ../../../be/docs/adr/ADR-0007-category-type-aligns-with-content-type.md
  - ../../../be/docs/adr/ADR-0012-remove-audio-story-canonical-type.md
  - ../../../be/src/main/java/com/tellpal/v2/category/domain/CategoryType.java
  - ../../../be/src/main/java/com/tellpal/v2/category/web/admin/CategoryAdminController.java
  - ../../../be/src/main/java/com/tellpal/v2/category/application/CategoryContentReferenceValidator.java
sources:
  - /Users/gurhankucuk/Documents/GitHub/tellpalv2/yuklenecek_hikayeler/tellpal_public_categories.csv
---

> **Canonical contract.** This SPEC and the files in `companions:` are the complete, preservation-validated contract for importing the legacy multilingual public categories into the v2 category model.

# Multilingual Category Import

## Why

The legacy public-category CSV contains 105 localized rows whose source IDs do not express multilingual identity, while the v2 model requires one category aggregate with language-scoped localizations. The import must preserve editorial category boundaries, keep Audio Books separately discoverable, and avoid reintroducing `AUDIO_STORY` as a canonical category type.

## Capabilities

- **CAP-1**
  - **intent:** The importer validates the category CSV before any mutation.
  - **success:** A dry run deterministically reports schema, encoding, required text, supported-language, source-ID, type, and row-assignment validation results.

- **CAP-2**
  - **intent:** The importer maps source rows into explicit category groups and language localizations.
  - **success:** Every source row is assigned exactly once to a reviewed group or an explicit unresolved bucket; grouping never relies on an image URL alone.

- **CAP-3**
  - **intent:** The importer creates or reconciles category aggregates and localizations idempotently.
  - **success:** Repeating the import does not create duplicate slugs or duplicate category-language localizations, and the source-ID mapping remains auditable.

- **CAP-4**
  - **intent:** The importer preserves Audio Books as a separate category while keeping canonical category compatibility valid.
  - **success:** Source rows `29`, `31`, `38`, and `163` become one distinct `audio-books` category with `STORY` canonical type and four localizations; no canonical `AUDIO_STORY` category row is written.

- **CAP-5**
  - **intent:** The importer resolves category images into v2 media references or reports unresolved image work.
  - **success:** A localization receives `imageMediaId` only after valid backend-mediated asset registration, and token-bearing source URLs are absent from persisted import artifacts.

## Constraints

- Canonical category types are `STORY`, `MEDITATION`, and `LULLABY`; source `AUDIO_STORY` is retained as provenance and discovery meaning, not as a persisted canonical type.
- Category grouping is represented by one aggregate and one stable slug with language-scoped localizations; the current model has no parent, family, or group-ID field.
- Category curation accepts content whose canonical content type matches the category type. This CSV contains no category-content links or display orders, so curation is a separate phase.
- Audio Books curation must select `STORY` localizations that are audio-ready. The current category validator checks canonical type, active state, and published localization but does not enforce narration readiness.
- Source `image_url` values are transient asset inputs only; v2 category localizations store positive `imageMediaId` values.
- The source has no slug, premium, active, status, or published timestamp values. The importer must require an explicit policy or leave the corresponding records in a reviewable state.
- The provisional mapping contains 33 candidate category aggregates and 105 localizations; ambiguous editorial groups remain open until reviewed.

## Non-goals

- Restoring `AUDIO_STORY` as a canonical content, category, or asset-processing type.
- Creating or migrating legacy independent `AUDIO_STORY` content records.
- Importing category-content membership or display order from this CSV.
- Inferring multilingual groups solely from names, image filenames, or reused image assets.
- Publishing categories, assigning premium access, or choosing active-state policy without explicit import policy.
- Redesigning the CMS category UI or implementing the broader story-audio experience contract.

## Success signal

The reviewed CSV can be dry-run and imported into v2 as deterministic category aggregates and localizations, with all 105 source rows accounted for, no unsupported canonical type written, no duplicate category-language records on rerun, and an auditable source-to-v2 mapping. Audio Books appears as its own category identity while only audio-ready STORY localizations are eligible for its later curation.

## Assumptions

- The current scope creates category aggregates and localizations; category-content curation follows after content IDs and audio readiness are known.
- The four Audio Books rows are one multilingual category candidate, not four independent categories.
- Token-bearing Firebase image URLs can be used transiently to obtain assets but are not committed, persisted, or emitted in reports.

## Open Questions

- Should imported localizations start as `DRAFT`, or should the importer publish them with supplied publication timestamps?
- What are the intended default values for `premium` and `active` for this public-category import?
- Are the provisional 33 group mappings accepted, especially Favorites, Parent Favorites, Editors' Pick, Sharing/Solidarity, Imagination/Creativity, and the single-language categories?
- Should this rollout only create the Audio Books category, or also validate and attach audio-ready STORY content?


--- untracked file: _bmad-output/specs/spec-category-import/category-import-mapping.md ---
# Category Import Mapping

This companion records the source profile, target-field mapping, provisional multilingual grouping, and review blockers for `tellpal_public_categories.csv`.

## Source profile

| Measure | Result |
|---|---|
| Source columns | `id`, `name`, `description`, `language`, `type`, `image_url` |
| Rows | 105 |
| Languages | `tr` 30, `de` 26, `pt` 25, `en` 24 |
| Source types | `STORY` 93, `MEDITATION` 4, `LULLABY` 4, `AUDIO_STORY` 4 |
| Unique source IDs | 105 |
| Candidate aggregates | 33 |
| Candidate localizations | 105 |

## Target mapping

| Source field or concept | v2 target | Rule |
|---|---|---|
| `id` | External import mapping only | Do not use as v2 category ID; retain for audit and rerun reconciliation. |
| `name` | `category_localizations.name` | Trim whitespace; keep the source language and editorial copy. |
| `description` | `category_localizations.description` | Trim whitespace; preserve non-empty descriptions. |
| `language` | `category_localizations.language_code` | Supported values in this file are `tr`, `en`, `pt`, and `de`. |
| `type=STORY` | `categories.type=STORY` | Direct canonical mapping. |
| `type=MEDITATION` | `categories.type=MEDITATION` | Direct canonical mapping. |
| `type=LULLABY` | `categories.type=LULLABY` | Direct canonical mapping. |
| `type=AUDIO_STORY` | `categories.type=STORY` plus `audio-books` identity | Preserve `AUDIO_STORY` in source provenance; do not write it to the canonical type column. |
| `image_url` | `category_localizations.image_media_id` | Register or reuse a v2 media asset first; never persist the token-bearing URL. |
| Derived group key | `categories.slug` | One stable slug per reviewed multilingual group. |
| Missing `premium` | Import policy | Must be explicitly supplied; do not infer from the CSV. |
| Missing `active` | Import policy | Must be explicitly supplied; do not infer from the CSV. |
| Missing `status` | Localization import policy | `DRAFT` is safe only if accepted as policy; `PUBLISHED` requires `publishedAt`. |
| Missing `publishedAt` | Localization import policy | Required whenever status is `PUBLISHED`. |

## Provisional group candidates

The source IDs below are assigned exactly once. `High` means the names/descriptions are strong translation or localization matches. `Review` means the semantic match is plausible but needs editorial approval.

| Candidate slug | Source IDs | Languages | Confidence / note |
|---|---:|---|---|
| `audio-books` | 29, 31, 38, 163 | tr, en, pt, de | High; separate category identity, canonical type `STORY`. |
| `lullaby-and-relaxing-music` | 14, 15, 45, 156 | tr, en, pt, de | High. |
| `meditation` | 28, 32, 37, 162 | tr, en, pt, de | High. |
| `popular-favorites` | 1, 24, 84, 150 | tr, en, pt, de | Review; editorial wording differs by locale. |
| `family-and-friendship` | 13, 16, 88, 157 | en, tr, pt, de | High. |
| `empathy-and-tolerance` | 54, 55, 99, 169 | tr, en, pt, de | High. |
| `activities-and-fun` | 17, 18, 87, 158 | tr, en, pt, de | High. |
| `informative-stories` | 5, 19, 86, 153 | tr, en, pt, de | High. |
| `nature-and-discovery` | 10, 11, 89, 155 | tr, en, pt, de | High. |
| `sleeping-stories` | 22, 30, 82, 160 | tr, en, pt, de | High. |
| `serial-stories` | 34, 35, 81, 164 | tr, en, pt, de | High. |
| `earthworm-series` | 3, 60, 92, 152 | tr, en, pt, de | High. |
| `sharing-and-cooperation` | 50, 59, 98, 165 | tr, en, pt, de | Review; Portuguese uses `Solidariedade`. |
| `emotional-awareness` | 51, 58, 94, 166 | tr, en, pt, de | High. |
| `problem-solving` | 52, 57, 100, 167 | tr, en, pt, de | High. |
| `confidence-and-courage` | 53, 56, 101, 168 | tr, en, pt, de | High. |
| `science` | 61, 62, 97, 170 | tr, en, pt, de | Review; Portuguese adds technology. |
| `imagination-and-creativity` | 64, 66, 95, 171 | tr, en, pt, de | Review; Portuguese shortens the label to imagination. |
| `competition` | 65, 67, 96, 172 | tr, en, pt, de | High. |
| `magic-words` | 102, 103, 106, 173 | pt, tr, en, de | High. |
| `animated-stories` | 20, 21, 159 | tr, en, de | Partial; Portuguese localization absent. |
| `stories-with-songs` | 27, 93, 161 | tr, pt, de | Partial; English localization absent. |
| `brand-new-stories` | 104, 105, 174 | tr, en, de | Partial; Portuguese localization absent. |
| `animal-friends` | 2, 151 | tr, de | Partial; English and Portuguese matches not found. |
| `parent-favorites` | 6, 154 | tr, de | Partial; English and Portuguese matches not found. |
| `editors-pick` | 9, 90 | en, pt | Partial; Turkish and German matches not found. |
| `english-stories` | 7, 91 | tr, pt | Partial; English and German matches not found. |
| `adventure-stories` | 25, 83 | en, pt | Partial; Turkish and German matches not found. |
| `hello-summer` | 107, 175 | tr, de | Partial; English and Portuguese matches not found. |
| `choice-stories` | 4 | tr | Single-language category. |
| `special-occasions` | 12 | tr | Single-language category. |
| `recommendations` | 26 | tr | Single-language category. |
| `library-discovery` | 80 | pt | Single-language category. |

## Data-quality findings

- Image paths are not a safe grouping key: 17 image paths are reused across unrelated concepts. `doğa ve canlılar.png` spans nature, science, serial, sharing, and earthworm rows; `aktivite ve eğlence.png` spans activities, serial, science, magic words, and competition rows.
- Source row `37` (Portuguese Meditation) points to the audio-books image; source row `38` (Portuguese Audio Books) points to the meditation image.
- Source row `15` has a leading space in its description; source row `35` has a trailing space. The importer must normalize surrounding whitespace.
- The four `AUDIO_STORY` rows are semantically one separate Audio Books category candidate, not four canonical category types.

## Import boundary

1. Validate and normalize the CSV without writing v2 data.
2. Produce a reviewed mapping manifest with one group per stable slug and one localization per source row.
3. Resolve source images through backend-mediated media registration and record the resulting asset IDs.
4. Create or reconcile category aggregates by slug.
5. Create or reconcile localizations by category and language.
6. Emit source-to-category and source-to-localization audit mappings.
7. Handle category-content membership, display order, and Audio Books narration-readiness filtering in a later curation phase.

The current admin API exposes category and localization operations separately; no bulk category-import endpoint is part of the current contract.


Do not invoke any skill. If the instruction file is unreadable, report that exact failure and stop. Return only the review result.
