from __future__ import annotations

import argparse
import shutil
import sys
import tempfile
from pathlib import Path

from audio_story_import_workflow import format_preview
from audio_story_manifest import StoryValidationError, build_audio_story_plan, write_status_csv


def main() -> int:
    _configure_console_encoding()
    parser = argparse.ArgumentParser(description="Inspect Firebase audio-story rows without Admin API writes.")
    _add_arguments(parser)
    arguments = parser.parse_args()
    cache_directory = Path(tempfile.mkdtemp(prefix="tellpal-audio-story-inspect-"))
    try:
        plan = build_audio_story_plan(**_plan_arguments(arguments), cache_directory=cache_directory)
        write_status_csv(plan)
        print(format_preview(plan))
        return 1 if any(row.status == "ERROR" for row in plan.rows) else 0
    except (StoryValidationError, ValueError, RuntimeError, OSError) as exception:
        print(f"Audio-story preflight failed: {exception}", file=sys.stderr)
        return 2
    finally:
        shutil.rmtree(cache_directory, ignore_errors=True)


def _plan_arguments(arguments: argparse.Namespace) -> dict[str, object]:
    return {
        "csv_path": arguments.csv_path,
        "storage_base_url": arguments.storage_base_url,
        "storage_bucket": arguments.storage_bucket,
        "cover_prefix": arguments.cover_prefix,
        "audio_prefix": arguments.audio_prefix,
        "status_csv_path": arguments.status_csv,
        "service_account_json": arguments.service_account_json,
        "timeout_seconds": arguments.timeout_seconds,
    }


def _add_arguments(parser: argparse.ArgumentParser) -> None:
    parser.add_argument("csv_path", help="Path to audio_stories.csv")
    parser.add_argument("--storage-base-url", default="https://storage.googleapis.com")
    parser.add_argument("--storage-bucket", default="tellpal-ee0dd.appspot.com")
    parser.add_argument("--cover-prefix", default="cover_images")
    parser.add_argument("--audio-prefix", default="")
    parser.add_argument("--service-account-json", help="Path to a service-account JSON for private GCS objects")
    parser.add_argument("--status-csv", help="Output sidecar status CSV; defaults to <csv-stem>.import.csv")
    parser.add_argument("--timeout-seconds", type=float, default=120)


def _configure_console_encoding() -> None:
    for stream in (sys.stdout, sys.stderr):
        reconfigure = getattr(stream, "reconfigure", None)
        if reconfigure:
            reconfigure(encoding="utf-8", errors="replace")


if __name__ == "__main__":
    raise SystemExit(main())
