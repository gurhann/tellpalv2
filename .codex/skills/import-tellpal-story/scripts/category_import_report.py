from __future__ import annotations

import json
import os
import re
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
        self.state["error"] = {"type": type(exception).__name__, "message": _safe_error_message(exception)}
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


def _safe_error_message(exception: BaseException) -> str:
    message = str(exception)
    message = re.sub(r"https?://[^\s'\"}]+", "<redacted-url>", message, flags=re.IGNORECASE)
    message = re.sub(r"(?i)category_images/[^'\"}]+", "<redacted-object-path>", message)
    return message[:500]
