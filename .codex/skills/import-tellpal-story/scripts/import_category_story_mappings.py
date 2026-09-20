from __future__ import annotations

import argparse
import getpass
import os
import sys

from category_story_mapping_manifest import (
    CategoryStoryMappingValidationError,
    build_category_story_mapping_plan,
)
from category_story_mapping_report import CategoryStoryMappingRunReport
from category_story_mapping_workflow import (
    CategoryStoryMappingImportError,
    execute_mapping_import,
    format_preview,
    remote_preflight,
    require_ready,
)
from tellpal_admin_client import (
    AdminApiError,
    AdminTransportError,
    TellPalAdminClient,
    validate_base_url,
)


def main() -> int:
    _configure_console_encoding()
    parser = argparse.ArgumentParser(
        description="Import TellPal story-to-category curation mappings through the Admin API."
    )
    parser.add_argument("csv_path", help="Path to category_f_stories.csv")
    parser.add_argument("--timeout-seconds", type=float, default=120)
    arguments = parser.parse_args()

    report: CategoryStoryMappingRunReport | None = None
    client: TellPalAdminClient | None = None
    try:
        if not sys.stdin.isatty():
            raise RuntimeError("Live category mapping import requires an interactive terminal")
        api_base_url = validate_base_url(_required_environment("TELLPAL_API_BASE_URL"))
        username = _required_environment("TELLPAL_ADMIN_USERNAME")
        plan = build_category_story_mapping_plan(arguments.csv_path)
        report = CategoryStoryMappingRunReport(plan, api_base_url)
        client = TellPalAdminClient(api_base_url, timeout_seconds=arguments.timeout_seconds)
        password = getpass.getpass(f"TellPal admin password for {username}: ")
        if not password:
            raise RuntimeError("Admin password must not be empty")
        client.login(username, password)
        del password

        remote = remote_preflight(plan, client)
        report.record_remote_preflight(remote)
        print()
        print(format_preview(plan, remote))
        require_ready(remote)
        print()
        if input("Type 'import' to start writes: ").strip() != "import":
            report.mark_cancelled()
            print(f"Import cancelled. Run report: {report.result_path}")
            return 1

        remote = remote_preflight(plan, client)
        report.record_remote_preflight(remote)
        require_ready(remote)
        summary = execute_mapping_import(plan, client, report, remote)
        print()
        print(
            f"Import completed: lanes={summary['lanes']}, "
            f"publishedCategoryLocalizations={summary['publishedCategoryLocalizations']}, "
            f"createdLinks={summary['createdLinks']}, reusedLinks={summary['reusedLinks']}"
        )
        print(f"Run report: {report.result_path}")
        return 0
    except KeyboardInterrupt as exception:
        if report is not None:
            if client is not None:
                report.record_last_request(client.last_request)
            report.mark_failure(exception)
            print(f"Import interrupted. Run report: {report.result_path}", file=sys.stderr)
        return 130
    except (
        CategoryStoryMappingValidationError,
        CategoryStoryMappingImportError,
        AdminApiError,
        AdminTransportError,
        RuntimeError,
        OSError,
    ) as exception:
        if report is not None:
            if client is not None:
                report.record_last_request(client.last_request)
            report.mark_failure(exception)
            print(f"Run report: {report.result_path}", file=sys.stderr)
        print(f"Category/story mapping import failed: {exception}", file=sys.stderr)
        return 2
    finally:
        if client is not None:
            try:
                client.logout()
            except Exception as exception:
                print(f"Warning: logout failed: {exception}", file=sys.stderr)


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
