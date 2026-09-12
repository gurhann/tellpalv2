from __future__ import annotations

import argparse
import shutil
import sys
import tempfile
from pathlib import Path

from meditation_import_workflow import format_preview
from meditation_manifest import StoryValidationError, build_meditation_plan


def main() -> int:
    _configure_console_encoding()
    parser = argparse.ArgumentParser(description="Inspect legacy TellPal meditations without API writes.")
    _add_arguments(parser)
    arguments = parser.parse_args()
    cache_directory = Path(tempfile.mkdtemp(prefix="tellpal-meditation-inspect-"))
    try:
        plan = build_meditation_plan(**_plan_arguments(arguments), cache_directory=cache_directory)
        print(format_preview(plan))
        return 0
    except (StoryValidationError, ValueError, RuntimeError, OSError) as exception:
        print(f"Meditation preflight failed: {exception}", file=sys.stderr)
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
        "external_key": arguments.external_key,
        "service_account_json": arguments.service_account_json,
        "timeout_seconds": arguments.timeout_seconds,
        "body_sources": _parse_body_sources(arguments.body_source),
        "body_directory": arguments.body_directory,
    }


def _add_arguments(parser: argparse.ArgumentParser) -> None:
    parser.add_argument("csv_path", help="Path to meditations.csv")
    parser.add_argument("--storage-base-url", default="https://storage.googleapis.com")
    parser.add_argument("--storage-bucket", default="tellpal-ee0dd.appspot.com")
    parser.add_argument("--cover-prefix", default="cover_images")
    parser.add_argument("--audio-prefix", default="")
    parser.add_argument("--external-key", help="Inspect only one generated meditation external key")
    parser.add_argument("--service-account-json", help="Path to a service-account JSON for private GCS objects")
    parser.add_argument(
        "--body-directory",
        "--body-dir",
        dest="body_directory",
        help="Directory containing <group>/<language>.txt or .docx sources",
    )
    parser.add_argument(
        "--body-source",
        action="append",
        default=[],
        metavar="GROUP/LANGUAGE=PATH",
        help="Explicit body source mapping; may be repeated",
    )
    parser.add_argument("--timeout-seconds", type=float, default=120)
    parser.add_argument("--inactive", action="store_true")
    parser.add_argument("--no-publish", action="store_true")


def _parse_body_sources(values: list[str]) -> dict[tuple[str, str], str]:
    result: dict[tuple[str, str], str] = {}
    for value in values:
        if "=" not in value:
            raise ValueError("--body-source must use GROUP/LANGUAGE=PATH")
        key, path = value.split("=", 1)
        pieces = key.replace("::", "/").split("/", 1)
        if len(pieces) != 2 or not pieces[0].strip() or not pieces[1].strip():
            raise ValueError("--body-source must use GROUP/LANGUAGE=PATH")
        mapping_key = (pieces[0].strip(), pieces[1].strip())
        if mapping_key in result:
            raise ValueError(f"Duplicate --body-source mapping: {key}")
        result[mapping_key] = path
    return result


def _configure_console_encoding() -> None:
    for stream in (sys.stdout, sys.stderr):
        reconfigure = getattr(stream, "reconfigure", None)
        if reconfigure:
            reconfigure(encoding="utf-8", errors="replace")


if __name__ == "__main__":
    raise SystemExit(main())
