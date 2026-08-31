from __future__ import annotations

import argparse
import json
import sys

from story_import_workflow import format_preview
from story_manifest import StoryValidationError, build_story_plan


def main() -> int:
    _configure_console_encoding()
    parser = argparse.ArgumentParser(
        description="Validate a prepared TellPal story folder without calling the Admin API."
    )
    parser.add_argument("story_directory", help="Prepared story directory containing metadata.csv")
    parser.add_argument("--json", action="store_true", help="Print the complete plan as JSON")
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
    arguments = parser.parse_args()

    try:
        plan = build_story_plan(
            arguments.story_directory,
            textless_page_overrides=_parse_textless_page_overrides(arguments.textless_page),
            metadata_row_overrides=_parse_metadata_row_overrides(arguments.metadata_row),
            description_overrides=_parse_description_overrides(arguments.description),
            allow_missing_textless=arguments.allow_missing_textless,
        )
    except StoryValidationError as exception:
        print(f"Preflight failed: {exception}", file=sys.stderr)
        return 2

    if arguments.json:
        print(json.dumps(plan.to_dict(), ensure_ascii=False, indent=2))
    else:
        print(format_preview(plan))
    return 0


def _parse_textless_page_overrides(values: list[str]) -> dict[int, str]:
    result: dict[int, str] = {}
    for value in values:
        page_text, separator, path = value.partition("=")
        if not separator or not page_text.strip().isdigit() or not path.strip():
            raise StoryValidationError(
                f"Invalid --textless-page value {value!r}; expected PAGE=RELATIVE_PATH"
            )
        page_number = int(page_text.strip())
        if page_number in result:
            raise StoryValidationError(f"Duplicate --textless-page override for page {page_number}")
        result[page_number] = path.strip()
    return result


def _parse_metadata_row_overrides(values: list[str]) -> dict[str, str]:
    result: dict[str, str] = {}
    for value in values:
        language, separator, legacy_id = value.partition("=")
        language = language.strip().casefold()
        legacy_id = legacy_id.strip()
        if not separator or not language or not legacy_id:
            raise StoryValidationError(
                f"Invalid --metadata-row value {value!r}; expected LANGUAGE=LEGACY_ID"
            )
        if language in result:
            raise StoryValidationError(f"Duplicate --metadata-row override for {language}")
        result[language] = legacy_id
    return result


def _parse_description_overrides(values: list[str]) -> dict[str, str | None]:
    result: dict[str, str | None] = {}
    for value in values:
        language, separator, description = value.partition("=")
        language = language.strip().casefold()
        if not separator or not language:
            raise StoryValidationError(
                f"Invalid --description value {value!r}; expected LANGUAGE=TEXT"
            )
        if language in result:
            raise StoryValidationError(f"Duplicate --description override for {language}")
        normalized = description.strip()
        result[language] = normalized or None
    return result


def _configure_console_encoding() -> None:
    for stream in (sys.stdout, sys.stderr):
        reconfigure = getattr(stream, "reconfigure", None)
        if reconfigure:
            reconfigure(encoding="utf-8", errors="replace")


if __name__ == "__main__":
    raise SystemExit(main())
