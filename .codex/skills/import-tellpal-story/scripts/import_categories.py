from __future__ import annotations

import argparse
import getpass
import os
import sys

from category_import_report import CategoryImportRunReport
from category_import_workflow import (
    CategoryImportError,
    execute_import,
    format_preview,
    remote_preflight,
)
from category_manifest import CategoryValidationError, build_category_plan, verify_source_images
from tellpal_admin_client import (
    AdminApiError,
    AdminTransportError,
    TellPalAdminClient,
    validate_base_url,
)


def main() -> int:
    _configure_console_encoding()
    parser = argparse.ArgumentParser(description="Import legacy TellPal categories through the Admin API.")
    parser.add_argument("csv_path", help="Path to tellpal_public_categories.csv")
    parser.add_argument("--mapping", help="Path to the approved category mapping JSON")
    parser.add_argument("--timeout-seconds", type=float, default=120)
    arguments = parser.parse_args()

    report: CategoryImportRunReport | None = None
    client: TellPalAdminClient | None = None
    try:
        if not sys.stdin.isatty():
            raise RuntimeError("Live category import requires an interactive terminal")
        api_base_url = validate_base_url(_required_environment("TELLPAL_API_BASE_URL"))
        username = _required_environment("TELLPAL_ADMIN_USERNAME")
        plan = build_category_plan(arguments.csv_path, mapping_path=arguments.mapping)
        report = CategoryImportRunReport(plan, api_base_url)
        verify_source_images(plan, timeout_seconds=arguments.timeout_seconds)
        client = TellPalAdminClient(api_base_url, timeout_seconds=arguments.timeout_seconds)
        password = getpass.getpass(f"TellPal admin password for {username}: ")
        if not password:
            raise RuntimeError("Admin password must not be empty")
        client.login(username, password)
        del password

        remote = remote_preflight(plan, client)
        print()
        print(format_preview(plan, remote))
        print()
        if input("Type 'import' to start writes: ") != "import":
            report.mark_cancelled()
            print(f"Import cancelled. Run report: {report.result_path}")
            return 1

        remote = remote_preflight(plan, client)
        summary = execute_import(plan, client, report, remote)
        print()
        print(
            f"Import completed: groups={summary['groups']}, "
            f"createdLocalizations={summary['createdLocalizations']}, "
            f"reusedLocalizations={summary['reusedLocalizations']}"
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
    except (CategoryValidationError, CategoryImportError, AdminApiError, AdminTransportError, RuntimeError, OSError) as exception:
        if report is not None:
            if client is not None:
                report.record_last_request(client.last_request)
            report.mark_failure(exception)
            print(f"Run report: {report.result_path}", file=sys.stderr)
        print(f"Category import failed: {exception}", file=sys.stderr)
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
