from __future__ import annotations

import argparse
import getpass
import os
import shutil
import sys
import tempfile
from pathlib import Path

from meditation_import_report import MeditationImportRunReport
from meditation_import_workflow import (
    MeditationRemotePreflightError,
    execute_import,
    format_preview,
    remote_preflight,
)
from meditation_manifest import StoryValidationError, build_meditation_plan
from tellpal_admin_client import TellPalAdminClient, validate_base_url


def main() -> int:
    _configure_console_encoding()
    parser = argparse.ArgumentParser(description="Import legacy TellPal meditations through the Admin API.")
    _add_arguments(parser)
    arguments = parser.parse_args()
    report: MeditationImportRunReport | None = None
    client: TellPalAdminClient | None = None
    cache_directory: Path | None = None
    try:
        if not sys.stdin.isatty():
            raise RuntimeError(
                "Live meditation import requires an interactive terminal for the masked password and approval prompt"
            )
        api_base_url = validate_base_url(_required_environment("TELLPAL_API_BASE_URL"))
        username = _required_environment("TELLPAL_ADMIN_USERNAME")
        cache_directory = Path(tempfile.mkdtemp(prefix="tellpal-meditation-import-"))
        plan = build_meditation_plan(**_plan_arguments(arguments), cache_directory=cache_directory)
        report = MeditationImportRunReport(plan, api_base_url)
        if plan.missing_body_sources:
            raise MeditationRemotePreflightError(
                "Live import unavailable before login; body text is missing for: "
                + ", ".join(plan.missing_body_sources)
            )
        client = TellPalAdminClient(api_base_url, timeout_seconds=arguments.timeout_seconds)
        password = getpass.getpass(f"TellPal admin password for {username}: ")
        if not password:
            raise RuntimeError("Admin password must not be empty")
        client.login(username, password)
        del password
        report.set_phase("remote-preflight")
        remote_preflight(plan, client)
        print()
        print(format_preview(plan, api_base_url=api_base_url))
        print()
        entered = input("Type 'import' to start writes: ")
        if entered.casefold() != "import":
            report.mark_cancelled()
            print(f"Import cancelled. Run report: {report.result_path}")
            return 1
        report.set_phase("remote-preflight")
        remote_preflight(plan, client)
        summary = execute_import(plan, client, report)
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
    except EOFError as exception:
        error = RuntimeError("Approval input ended before confirmation")
        if report is not None:
            if client is not None:
                report.record_last_request(client.last_request)
            report.mark_failure(error)
            print(f"Run report: {report.result_path}", file=sys.stderr)
        print(f"Meditation import failed: {error}", file=sys.stderr)
        return 2
    except (StoryValidationError, ValueError, RuntimeError, OSError) as exception:
        if report is not None:
            if client is not None:
                report.record_last_request(client.last_request)
            report.mark_failure(exception)
            print(f"Run report: {report.result_path}", file=sys.stderr)
        print(f"Meditation import failed: {exception}", file=sys.stderr)
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
    parser.add_argument("--external-key", help="Import only one generated meditation external key")
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
