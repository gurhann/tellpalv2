from __future__ import annotations

import argparse
import sys

from category_manifest import CategoryValidationError, build_category_plan
from category_import_workflow import format_preview


def main() -> int:
    _configure_console_encoding()
    parser = argparse.ArgumentParser(description="Inspect legacy TellPal categories without API writes.")
    parser.add_argument("csv_path", help="Path to tellpal_public_categories.csv")
    parser.add_argument("--mapping", help="Path to the approved category mapping JSON")
    arguments = parser.parse_args()
    try:
        plan = build_category_plan(arguments.csv_path, mapping_path=arguments.mapping)
        print(format_preview(plan))
        return 0
    except (CategoryValidationError, ValueError, OSError) as exception:
        print(f"Category preflight failed: {exception}", file=sys.stderr)
        return 2


def _configure_console_encoding() -> None:
    for stream in (sys.stdout, sys.stderr):
        reconfigure = getattr(stream, "reconfigure", None)
        if reconfigure:
            reconfigure(encoding="utf-8", errors="replace")


if __name__ == "__main__":
    raise SystemExit(main())
