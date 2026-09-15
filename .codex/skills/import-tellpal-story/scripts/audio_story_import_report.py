from __future__ import annotations

import json
import os
import uuid
from datetime import UTC, datetime
from pathlib import Path
from typing import Any

from audio_story_manifest import AudioStoryPlan, write_status_csv


class AudioStoryImportRunReport:
    """Persists non-secret audio-story import diagnostics and row outcomes."""

    def __init__(self, plan: AudioStoryPlan, api_base_url: str):
        run_id = f"{datetime.now(UTC).strftime('%Y%m%dT%H%M%SZ')}-{uuid.uuid4().hex[:8]}"
        self.directory = run_storage_root() / run_id
        self.directory.mkdir(parents=True, exist_ok=False)
        self.manifest_path = self.directory / "manifest.json"
        self.result_path = self.directory / "result.json"
        self.state: dict[str, Any] = {
            "runId": run_id,
            "createdAt": utc_now(),
            "status": "PREVIEW",
            "phase": "local-preflight",
            "apiBaseUrl": api_base_url,
            "csvPath": plan.csv_path,
            "statusCsvPath": plan.status_csv_path,
            "sourceFingerprint": plan.source_fingerprint,
            "contentIds": [],
            "uploadedAssets": [],
            "rows": [],
            "completedSteps": [],
            "lastRequest": None,
            "error": None,
        }
        atomic_write_json(self.manifest_path, plan.to_dict())
        self.flush()

    def set_phase(self, phase: str) -> None:
        self.state["phase"] = phase
        self.flush()

    def add_content_id(self, content_id: int) -> None:
        if content_id not in self.state["contentIds"]:
            self.state["contentIds"].append(content_id)
        self.flush()

    def record_asset(self, **asset: object) -> None:
        self.state["uploadedAssets"].append(asset)
        self.flush()

    def record_row(self, row: object) -> None:
        payload = {"lineNumber": row.line_number, **row.to_status_row()}
        self.state["rows"] = [item for item in self.state["rows"] if item.get("lineNumber") != row.line_number]
        self.state["rows"].append(payload)
        self.flush()

    def record_step(self, step: str) -> None:
        self.state["completedSteps"].append({"step": step, "at": utc_now()})
        self.flush()

    def record_last_request(self, request: dict[str, object] | None) -> None:
        self.state["lastRequest"] = request
        self.flush()

    def mark_running(self) -> None:
        self.state["status"] = "RUNNING"
        self.state["startedAt"] = utc_now()
        self.flush()

    def mark_cancelled(self) -> None:
        self.state["status"] = "CANCELLED"
        self.state["phase"] = "cancelled-before-write"
        self.state["cancelledAt"] = utc_now()
        self.flush()

    def mark_success(self, summary: dict[str, object]) -> None:
        self.state["status"] = "COMPLETED_WITH_ERRORS" if summary.get("errors") else "COMPLETED"
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
        return Path(local_app_data) / "TellPal" / "audio-story-import-agent" / "runs"
    return Path.home() / ".local" / "share" / "TellPal" / "audio-story-import-agent" / "runs"


def atomic_write_json(path: Path, payload: object) -> None:
    temporary = path.with_suffix(f"{path.suffix}.tmp")
    temporary.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    temporary.replace(path)


def utc_now() -> str:
    return datetime.now(UTC).isoformat()
