from __future__ import annotations

import argparse
import sys

from category_story_mapping_manifest import (
    CategoryStoryMappingValidationError,
    build_category_story_mapping_plan,
)
from category_story_mapping_workflow import format_preview


def main() -> int:
    _configure_console_encoding()
    parser = argparse.ArgumentParser(
        description="Inspect TellPal story-to-category mappings without API writes."
    )
    parser.add_argument("csv_path", help="Path to category_f_stories.csv")
    arguments = parser.parse_args()
    try:
        plan = build_category_story_mapping_plan(arguments.csv_path)
        print(format_preview(plan))
        _print_distribution(plan)
        return 0
    except (CategoryStoryMappingValidationError, ValueError, OSError) as exception:
        print(f"Category/story mapping preflight failed: {exception}", file=sys.stderr)
        return 2


def _print_distribution(plan) -> None:
    language_counts = {language: 0 for language in plan.languages}
    type_counts: dict[str, int] = {}
    for row in plan.rows:
        language_counts[row.language_code] += 1
        type_counts[row.source_type] = type_counts.get(row.source_type, 0) + 1
    print(f"Unique rows by language: {language_counts}")
    print(f"Unique rows by source type: {type_counts}")
    if plan.duplicates:
        print(f"First duplicate source rows: {[item.duplicate_row_number for item in plan.duplicates[:10]]}")
    if plan.ambiguity_risks:
        print(
            "Titles assigned to multiple category labels: "
            f"{len(plan.ambiguity_risks)}; this is valid many-to-many source data."
        )


def _configure_console_encoding() -> None:
    for stream in (sys.stdout, sys.stderr):
        reconfigure = getattr(stream, "reconfigure", None)
        if reconfigure:
            reconfigure(encoding="utf-8", errors="replace")


if __name__ == "__main__":
    raise SystemExit(main())
