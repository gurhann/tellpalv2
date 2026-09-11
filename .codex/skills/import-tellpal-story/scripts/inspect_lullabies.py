from __future__ import annotations

import argparse
import shutil
import sys
import tempfile
from pathlib import Path

from lullaby_import_workflow import format_preview
from lullaby_manifest import StoryValidationError, build_lullaby_plan


def main() -> int:
    _configure_console_encoding()
    parser = argparse.ArgumentParser(description="Download, validate, and preview legacy TellPal lullabies.")
    _add_arguments(parser)
    arguments = parser.parse_args()
    cache_directory = Path(tempfile.mkdtemp(prefix="tellpal-lullaby-import-"))
    try:
        plan = build_lullaby_plan(**_plan_arguments(arguments), cache_directory=cache_directory)
        print(format_preview(plan))
        print(f"  Source fingerprint: {plan.source_fingerprint}")
        return 0
    except (StoryValidationError, ValueError, OSError) as exception:
        print(f"Lullaby preflight failed: {exception}", file=sys.stderr)
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
        "active": not arguments.inactive,
        "publish": not arguments.no_publish,
        "duration_override": arguments.duration_minutes,
        "external_key": arguments.external_key,
        "service_account_json": arguments.service_account_json,
        "timeout_seconds": arguments.timeout_seconds,
    }


def _add_arguments(parser: argparse.ArgumentParser) -> None:
    parser.add_argument("csv_path", help="Path to lullabies.csv")
    parser.add_argument("--storage-base-url", default="https://storage.googleapis.com")
    parser.add_argument("--storage-bucket", default="tellpal-ee0dd.appspot.com")
    parser.add_argument("--cover-prefix", default="cover_images")
    parser.add_argument("--audio-prefix", default="")
    parser.add_argument("--duration-minutes", type=int)
    parser.add_argument("--external-key", help="Import only the generated lullaby external key")
    parser.add_argument("--service-account-json", help="Path to a service-account JSON used for private GCS objects")
    parser.add_argument("--timeout-seconds", type=float, default=120)
    parser.add_argument("--inactive", action="store_true")
    parser.add_argument("--no-publish", action="store_true")


def _configure_console_encoding() -> None:
    for stream in (sys.stdout, sys.stderr):
        reconfigure = getattr(stream, "reconfigure", None)
        if reconfigure:
            reconfigure(encoding="utf-8", errors="replace")


if __name__ == "__main__":
    raise SystemExit(main())
