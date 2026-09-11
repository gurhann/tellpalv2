from __future__ import annotations

import argparse
import getpass
import os
import shutil
import sys
import tempfile
from pathlib import Path

from lullaby_import_report import LullabyImportRunReport
from lullaby_import_workflow import execute_import, format_preview, remote_preflight
from lullaby_manifest import StoryValidationError, build_lullaby_plan
from story_import_models import ContributorResolution
from tellpal_admin_client import TellPalAdminClient, validate_base_url


def main() -> int:
    _configure_console_encoding()
    parser = argparse.ArgumentParser(description="Import legacy TellPal lullabies through the Admin API.")
    _add_arguments(parser)
    arguments = parser.parse_args()
    report: LullabyImportRunReport | None = None
    client: TellPalAdminClient | None = None
    cache_directory: Path | None = None
    try:
        if not sys.stdin.isatty():
            raise RuntimeError("Live lullaby import requires an interactive terminal for the masked password and approval prompt")
        api_base_url = validate_base_url(_required_environment("TELLPAL_API_BASE_URL"))
        username = _required_environment("TELLPAL_ADMIN_USERNAME")
        cache_directory = Path(tempfile.mkdtemp(prefix="tellpal-lullaby-import-"))
        plan = build_lullaby_plan(**_plan_arguments(arguments), cache_directory=cache_directory)
        report = LullabyImportRunReport(plan, api_base_url)
        client = TellPalAdminClient(api_base_url, timeout_seconds=arguments.timeout_seconds)
        password = getpass.getpass(f"TellPal admin password for {username}: ")
        if not password:
            raise RuntimeError("Admin password must not be empty")
        client.login(username, password)
        del password
        resolutions = remote_preflight(plan, client)
        print()
        print(format_preview(plan, api_base_url=api_base_url, contributor_resolutions=resolutions))
        print()
        entered = input("Type 'import' to start writes: ")
        if entered != "import":
            report.mark_cancelled()
            print(f"Import cancelled. Run report: {report.result_path}")
            return 1
        resolutions = remote_preflight(plan, client)
        summary = execute_import(plan, client, report, resolutions)
        print()
        print(f"Import completed: groups={summary['groups']}, contentIds={summary['contentIds']}")
        print(f"Run report: {report.result_path}")
        return 0
    except KeyboardInterrupt as exception:
        if report is not None:
            if client is not None:
                report.record_last_request(client.last_request)
            report.mark_failure(exception)
            print(f"Import interrupted. Run report: {report.result_path}", file=sys.stderr)
        return 130
    except (StoryValidationError, ValueError, RuntimeError, OSError) as exception:
        if report is not None:
            if client is not None:
                report.record_last_request(client.last_request)
            report.mark_failure(exception)
            print(f"Run report: {report.result_path}", file=sys.stderr)
        print(f"Lullaby import failed: {exception}", file=sys.stderr)
        return 2
    finally:
        if client is not None:
            try:
                client.logout()
            except Exception as exception:
                print(f"Warning: logout failed: {exception}", file=sys.stderr)
        if cache_directory is not None:
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
