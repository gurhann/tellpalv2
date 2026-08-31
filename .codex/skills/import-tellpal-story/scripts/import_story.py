from __future__ import annotations

import argparse
import getpass
import os
import sys

from story_import_report import ImportRunReport
from story_import_workflow import (
    assert_source_unchanged,
    execute_import,
    format_preview,
    remote_preflight,
)
from inspect_story import (
    _parse_description_overrides,
    _parse_metadata_row_overrides,
    _parse_textless_page_overrides,
)
from story_manifest import StoryValidationError, build_story_plan, normalize_key
from tellpal_admin_client import TellPalAdminClient, validate_base_url


def main() -> int:
    _configure_console_encoding()
    parser = argparse.ArgumentParser(
        description="Preflight and import one prepared story through the TellPal Admin API."
    )
    parser.add_argument("story_directory", help="Prepared story directory containing metadata.csv")
    parser.add_argument(
        "--timeout-seconds",
        type=float,
        default=120,
        help="Per-request timeout; mutating requests are never blindly retried",
    )
    parser.add_argument(
        "--textless-page",
        action="append",
        default=[],
        metavar="PAGE=RELATIVE_PATH",
        help="Explicitly map a textless page to a source image; may be repeated",
    )
    parser.add_argument(
        "--metadata-row",
        action="append",
        default=[],
        metavar="LANGUAGE=LEGACY_ID",
        help="Explicitly map a language to one metadata.csv legacy id; may be repeated",
    )
    parser.add_argument(
        "--description",
        action="append",
        default=[],
        metavar="LANGUAGE=TEXT",
        help="Override one localization description; use LANGUAGE= to send null",
    )
    parser.add_argument(
        "--allow-missing-textless",
        action="store_true",
        help="Explicitly import without textless media when the textless directory is empty or absent",
    )
    parser.add_argument(
        "--contributor-id",
        action="append",
        default=[],
        metavar="NAME=ID",
        help="Select an exact existing contributor when duplicate names exist; may be repeated",
    )
    arguments = parser.parse_args()

    report: ImportRunReport | None = None
    client: TellPalAdminClient | None = None
    exit_code = 0
    try:
        if not sys.stdin.isatty():
            raise RuntimeError(
                "Live import requires an interactive terminal for the masked password and approval prompt"
            )
        api_base_url = validate_base_url(_required_environment("TELLPAL_API_BASE_URL"))
        username = _required_environment("TELLPAL_ADMIN_USERNAME")
        plan = build_story_plan(
            arguments.story_directory,
            textless_page_overrides=_parse_textless_page_overrides(arguments.textless_page),
            metadata_row_overrides=_parse_metadata_row_overrides(arguments.metadata_row),
            description_overrides=_parse_description_overrides(arguments.description),
            allow_missing_textless=arguments.allow_missing_textless,
        )
        report = ImportRunReport(plan, api_base_url)
        client = TellPalAdminClient(api_base_url, timeout_seconds=arguments.timeout_seconds)

        password = getpass.getpass(f"TellPal admin password for {username}: ")
        if not password:
            raise RuntimeError("Admin password must not be empty")
        report.set_phase("authenticate")
        client.login(username, password)
        del password

        report.set_phase("remote-preflight")
        resolutions = remote_preflight(
            plan,
            client,
            contributor_id_overrides=_parse_contributor_id_overrides(arguments.contributor_id),
        )
        print()
        print(format_preview(plan, api_base_url=api_base_url, contributor_resolutions=resolutions))
        print()
        entered = input("Type 'import' to start writes: ")
        if not _is_import_confirmation(entered):
            report.mark_cancelled()
            print(f"Import cancelled. Run report: {report.result_path}")
            return 1

        assert_source_unchanged(plan)
        summary = execute_import(plan, client, report, resolutions)
        print()
        print(f"Import completed: contentId={summary['contentId']}, externalKey={summary['externalKey']}")
        print(f"Run report: {report.result_path}")
    except KeyboardInterrupt as exception:
        exit_code = 130
        if report is not None and report.state.get("status") not in {"COMPLETED", "CANCELLED"}:
            if client is not None:
                report.record_last_request(client.last_request)
            report.mark_failure(exception)
        print("Import interrupted by user", file=sys.stderr)
        if report is not None:
            print(f"Run report: {report.result_path}", file=sys.stderr)
    except (StoryValidationError, ValueError, RuntimeError, OSError) as exception:
        exit_code = 2
        if report is not None and report.state.get("status") not in {"COMPLETED", "CANCELLED"}:
            if client is not None:
                report.record_last_request(client.last_request)
            report.mark_failure(exception)
        print(f"Import failed: {exception}", file=sys.stderr)
        if report is not None:
            print(f"Run report: {report.result_path}", file=sys.stderr)
    finally:
        if client is not None:
            try:
                client.logout()
            except Exception as exception:
                print(f"Warning: logout failed: {exception}", file=sys.stderr)
    return exit_code


def _required_environment(name: str) -> str:
    value = os.environ.get(name, "").strip()
    if not value:
        raise RuntimeError(f"Required environment variable is missing: {name}")
    return value


def _parse_contributor_id_overrides(values: list[str]) -> dict[str, int]:
    result: dict[str, int] = {}
    for value in values:
        name, separator, contributor_id_text = value.rpartition("=")
        name = name.strip()
        contributor_id_text = contributor_id_text.strip()
        if not separator or not name or not contributor_id_text.isdigit():
            raise StoryValidationError(
                f"Invalid --contributor-id value {value!r}; expected NAME=ID"
            )
        contributor_id = int(contributor_id_text)
        if contributor_id <= 0:
            raise StoryValidationError("Contributor override ID must be positive")
        name_key = normalize_key(name)
        if name_key in result:
            raise StoryValidationError(f"Duplicate --contributor-id override for {name!r}")
        result[name_key] = contributor_id
    return result


def _is_import_confirmation(value: str) -> bool:
    return value.strip().casefold() == "import"


def _configure_console_encoding() -> None:
    for stream in (sys.stdout, sys.stderr):
        reconfigure = getattr(stream, "reconfigure", None)
        if reconfigure:
            reconfigure(encoding="utf-8", errors="replace")


if __name__ == "__main__":
    raise SystemExit(main())
