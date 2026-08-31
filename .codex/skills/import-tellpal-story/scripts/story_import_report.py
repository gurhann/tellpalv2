from __future__ import annotations

import json
import os
import uuid
from datetime import UTC, datetime
from pathlib import Path
from typing import Any

from story_manifest import StoryPlan


class ImportRunReport:
    """Persists non-secret run state for manual diagnosis without enabling resume."""

    def __init__(self, plan: StoryPlan, api_base_url: str):
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
            "storyDirectory": plan.story_directory,
            "externalKey": plan.external_key,
            "sourceFingerprint": plan.fingerprint,
            "contentId": None,
            "uploadedAssets": [],
            "completedSteps": [],
            "lastRequest": None,
            "error": None,
        }
        atomic_write_json(self.manifest_path, plan.to_dict())
        self.flush()

    def set_phase(self, phase: str) -> None:
        self.state["phase"] = phase
        self.flush()

    def set_content_id(self, content_id: int) -> None:
        self.state["contentId"] = content_id
        self.flush()

    def record_asset(
        self,
        *,
        asset_id: int,
        relative_path: str,
        kind: str,
        checksum_sha256: str,
        byte_size: int,
    ) -> None:
        self.state["uploadedAssets"].append(
            {
                "assetId": asset_id,
                "path": relative_path,
                "kind": kind,
                "checksumSha256": checksum_sha256,
                "byteSize": byte_size,
            }
        )
        self.flush()

    def record_step(self, step: str) -> None:
        self.state["completedSteps"].append({"step": step, "at": utc_now()})
        self.flush()

    def record_last_request(self, request: dict[str, object] | None) -> None:
        self.state["lastRequest"] = request

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
            "message": str(exception),
        }
        self.flush()

    def flush(self) -> None:
        atomic_write_json(self.result_path, self.state)


def run_storage_root() -> Path:
    local_app_data = os.environ.get("LOCALAPPDATA")
    if local_app_data:
        return Path(local_app_data) / "TellPal" / "story-import-agent" / "runs"
    return Path.home() / ".local" / "share" / "TellPal" / "story-import-agent" / "runs"


def atomic_write_json(path: Path, payload: object) -> None:
    temporary = path.with_suffix(f"{path.suffix}.tmp")
    temporary.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    temporary.replace(path)


def utc_now() -> str:
    return datetime.now(UTC).isoformat()
