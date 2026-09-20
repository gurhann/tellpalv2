from __future__ import annotations

import json
import os
import re
import uuid
from datetime import UTC, datetime
from pathlib import Path
from typing import Any

from category_story_mapping_manifest import CategoryStoryMappingPlan


class CategoryStoryMappingRunReport:
    """Persists token-free mapping state outside the repository."""

    def __init__(
        self,
        plan: CategoryStoryMappingPlan,
        api_base_url: str,
        *,
        storage_root: Path | None = None,
    ):
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
            "logicalRowCount": plan.logical_row_count,
            "uniqueAssignmentCount": plan.unique_assignment_count,
            "duplicateCount": plan.duplicate_count,
            "categoryLabelCount": plan.category_label_count,
            "languages": list(plan.languages),
            "categoryLocalizations": [],
            "links": [],
            "completedSteps": [],
            "lastRequest": None,
            "error": None,
        }
        atomic_write_json(self.manifest_path, plan.to_dict())
        self.flush()

    def record_remote_preflight(self, remote: Any) -> None:
        self.state["remotePreflight"] = remote.to_dict()
        self.flush()

    def mark_running(self) -> None:
        self.state["status"] = "RUNNING"
        self.state["phase"] = "mutating"
        self.state["startedAt"] = utc_now()
        self.flush()

    def record_category_localization(
        self,
        category_id: int,
        category_name: str,
        language_code: str,
        action: str,
    ) -> None:
        self.state["categoryLocalizations"].append(
            {
                "categoryId": category_id,
                "categoryName": category_name,
                "languageCode": language_code,
                "action": action,
            }
        )
        self.flush()

    def record_link(self, assignment: Any, action: str) -> None:
        self.state["links"].append(
            {
                "categoryId": assignment.category_id,
                "contentId": assignment.content_id,
                "languageCode": assignment.row.language_code,
                "categoryName": assignment.row.category_name,
                "storyTitle": assignment.row.story_title,
                "displayOrder": assignment.display_order,
                "sourceRowNumber": assignment.row.source_row_number,
                "action": action,
            }
        )
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
        self.state["error"] = {
            "type": type(exception).__name__,
            "message": _safe_error_message(exception),
        }
        self.flush()

    def flush(self) -> None:
        atomic_write_json(self.result_path, self.state)


def run_storage_root() -> Path:
    local_app_data = os.environ.get("LOCALAPPDATA")
    if local_app_data:
        return Path(local_app_data) / "TellPal" / "category-story-mapping-agent" / "runs"
    return Path.home() / ".local" / "share" / "TellPal" / "category-story-mapping-agent" / "runs"


def atomic_write_json(path: Path, payload: object) -> None:
    temporary = path.with_suffix(f"{path.suffix}.tmp")
    temporary.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    temporary.replace(path)


def utc_now() -> str:
    return datetime.now(UTC).isoformat()


def _safe_error_message(exception: BaseException) -> str:
    message = str(exception)
    message = re.sub(r"https?://[^\s'\"}]+", "<redacted-url>", message, flags=re.IGNORECASE)
    message = re.sub(r"(?i)(bearer\s+)[A-Za-z0-9._~+/=-]+", r"\1<redacted>", message)
    return message[:1000]


__all__ = ["CategoryStoryMappingRunReport", "run_storage_root"]
