from __future__ import annotations

import argparse
import getpass
import os
import shutil
import sys
import tempfile
from pathlib import Path

from audio_story_import_report import AudioStoryImportRunReport
from audio_story_import_workflow import AudioStoryRemotePreflightError, execute_import, format_preview, remote_preflight
from audio_story_manifest import AudioStoryPlan, StoryValidationError, build_audio_story_plan, write_status_csv
from tellpal_admin_client import TellPalAdminClient, validate_base_url


def main() -> int:
    _configure_console_encoding()
    parser = argparse.ArgumentParser(description="Import Firebase audio stories into existing TellPal STORY localizations.")
    _add_arguments(parser)
    arguments = parser.parse_args()
    if not sys.stdin.isatty():
        print("Live audio-story import requires an interactive terminal for the masked password and approval prompt", file=sys.stderr)
        return 2

    report: AudioStoryImportRunReport | None = None
    client: TellPalAdminClient | None = None
    cache_directory: Path | None = None
    try:
        api_base_url = validate_base_url(_required_environment("TELLPAL_API_BASE_URL"))
        username = _required_environment("TELLPAL_ADMIN_USERNAME")
        cache_directory = Path(tempfile.mkdtemp(prefix="tellpal-audio-story-import-"))
        plan = build_audio_story_plan(**_plan_arguments(arguments), cache_directory=cache_directory)
        write_status_csv(plan)
        report = AudioStoryImportRunReport(plan, api_base_url)
        client = TellPalAdminClient(api_base_url, timeout_seconds=arguments.timeout_seconds)

        password = getpass.getpass(f"TellPal admin password for {username}: ")
        if not password:
            raise RuntimeError("Admin password must not be empty")
        client.login(username, password)
        del password

        report.set_phase("remote-preflight")
        contexts = remote_preflight(plan, client)
        write_status_csv(plan)
        _record_rows(report, plan)
        print()
        print(format_preview(plan, api_base_url=api_base_url))
        print()
        entered = input("Type 'import' to start writes: ")
        if entered.casefold() != "import":
            report.mark_cancelled()
            print(f"Import cancelled. Run report: {report.result_path}")
            return 1

        contexts = remote_preflight(plan, client)
        write_status_csv(plan)
        _record_rows(report, plan)
        summary = execute_import(plan, client, report, contexts)
        print()
        print(
            "Import completed: "
            f"success={summary['success']}, alreadyImported={summary['alreadyImported']}, "
            f"errors={summary['errors']}, duplicates={summary['duplicates']}"
        )
        print(f"Status CSV: {plan.status_csv_path}")
        print(f"Run report: {report.result_path}")
        return 1 if summary["errors"] else 0
    except KeyboardInterrupt as exception:
        if report is not None:
            if client is not None:
                report.record_last_request(client.last_request)
            report.mark_failure(exception)
            print(f"Import interrupted. Run report: {report.result_path}", file=sys.stderr)
        return 130
    except EOFError as exception:
        error = RuntimeError("Approval input ended before confirmation")
        if report is not None:
            if client is not None:
                report.record_last_request(client.last_request)
            report.mark_failure(error)
            print(f"Run report: {report.result_path}", file=sys.stderr)
        print(f"Audio-story import failed: {error}", file=sys.stderr)
        return 2
    except (StoryValidationError, AudioStoryRemotePreflightError, ValueError, RuntimeError, OSError) as exception:
        if report is not None:
            if client is not None:
                report.record_last_request(client.last_request)
            report.mark_failure(exception)
            print(f"Run report: {report.result_path}", file=sys.stderr)
        print(f"Audio-story import failed: {exception}", file=sys.stderr)
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


def _required_environment(name: str) -> str:
    value = os.environ.get(name, "").strip()
    if not value:
        raise RuntimeError(f"Required environment variable is missing: {name}")
    return value


def _record_rows(report: AudioStoryImportRunReport, plan: AudioStoryPlan) -> None:
    for row in plan.rows:
        report.record_row(row)


def _configure_console_encoding() -> None:
    for stream in (sys.stdout, sys.stderr):
        reconfigure = getattr(stream, "reconfigure", None)
        if reconfigure:
            reconfigure(encoding="utf-8", errors="replace")


if __name__ == "__main__":
    raise SystemExit(main())
