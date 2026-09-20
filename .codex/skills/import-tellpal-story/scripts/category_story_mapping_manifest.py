from __future__ import annotations

import csv
import hashlib
import unicodedata
from collections import defaultdict
from dataclasses import dataclass
from pathlib import Path


SUPPORTED_LANGUAGES = {"tr", "en", "pt", "de"}
SUPPORTED_SOURCE_TYPES = {"STORY", "AUDIO_STORY", "MEDITATION", "LULLABY"}
CANONICAL_TYPE_BY_SOURCE = {"AUDIO_STORY": "STORY"}
EXPECTED_HEADER = ("language", "type", "name", "name")


class CategoryStoryMappingValidationError(ValueError):
    """Raised when the positional category-to-story CSV is unsafe to import."""


@dataclass(frozen=True)
class CategoryStoryMappingRow:
    source_row_number: int
    source_order: int
    language_code: str
    source_type: str
    canonical_type: str
    category_name: str
    story_title: str
    normalized_category_name: str
    normalized_story_title: str

    @property
    def assignment_key(self) -> tuple[str, str, str, str]:
        return (
            self.language_code,
            self.source_type,
            self.normalized_category_name,
            self.normalized_story_title,
        )

    @property
    def content_key(self) -> tuple[str, str, str]:
        return self.language_code, self.canonical_type, self.normalized_story_title


@dataclass(frozen=True)
class DuplicateCategoryStoryMapping:
    duplicate_row_number: int
    first_row_number: int
    assignment_key: tuple[str, str, str, str]


@dataclass(frozen=True)
class CategoryStoryAmbiguityRisk:
    language_code: str
    canonical_type: str
    normalized_story_title: str
    category_names: tuple[str, ...]
    source_row_numbers: tuple[int, ...]


@dataclass(frozen=True)
class CategoryStoryLanePlan:
    language_code: str
    category_name: str
    normalized_category_name: str
    canonical_type: str
    rows: tuple[CategoryStoryMappingRow, ...]


@dataclass(frozen=True)
class CategoryStoryMappingPlan:
    csv_path: str
    source_fingerprint: str
    raw_row_count: int
    rows: tuple[CategoryStoryMappingRow, ...]
    duplicates: tuple[DuplicateCategoryStoryMapping, ...]
    lanes: tuple[CategoryStoryLanePlan, ...]
    ambiguity_risks: tuple[CategoryStoryAmbiguityRisk, ...]

    @property
    def logical_row_count(self) -> int:
        return self.raw_row_count

    @property
    def row_count(self) -> int:
        return self.raw_row_count

    @property
    def unique_assignment_count(self) -> int:
        return len(self.rows)

    @property
    def duplicate_count(self) -> int:
        return len(self.duplicates)

    @property
    def languages(self) -> tuple[str, ...]:
        return tuple(sorted({row.language_code for row in self.rows}))

    @property
    def category_labels(self) -> tuple[str, ...]:
        return tuple(sorted({row.category_name for row in self.rows}))

    @property
    def category_label_count(self) -> int:
        return len(self.category_labels)

    @property
    def content_candidate_risk_count(self) -> int:
        return len(self.ambiguity_risks)

    def to_dict(self) -> dict[str, object]:
        """Return an audit-safe manifest without credentials, tokens, or remote URLs."""
        return {
            "csvPath": self.csv_path,
            "sourceFingerprint": self.source_fingerprint,
            "rawRowCount": self.raw_row_count,
            "uniqueAssignmentCount": self.unique_assignment_count,
            "duplicateCount": self.duplicate_count,
            "languages": list(self.languages),
            "categoryLabelCount": self.category_label_count,
            "ambiguityRiskCount": self.content_candidate_risk_count,
            "duplicates": [
                {
                    "duplicateRowNumber": item.duplicate_row_number,
                    "firstRowNumber": item.first_row_number,
                    "assignmentKey": list(item.assignment_key),
                }
                for item in self.duplicates
            ],
            "ambiguityRisks": [
                {
                    "languageCode": item.language_code,
                    "canonicalType": item.canonical_type,
                    "storyTitleKey": item.normalized_story_title,
                    "categoryNames": list(item.category_names),
                    "sourceRowNumbers": list(item.source_row_numbers),
                }
                for item in self.ambiguity_risks
            ],
            "lanes": [
                {
                    "languageCode": lane.language_code,
                    "categoryName": lane.category_name,
                    "canonicalType": lane.canonical_type,
                    "sourceRowNumbers": [row.source_row_number for row in lane.rows],
                    "storyTitles": [row.story_title for row in lane.rows],
                }
                for lane in self.lanes
            ],
        }


def build_category_story_mapping_plan(csv_path: str | Path) -> CategoryStoryMappingPlan:
    source = Path(csv_path).expanduser().resolve()
    if not source.is_file():
        raise CategoryStoryMappingValidationError(f"Category-story mapping CSV does not exist: {source}")

    raw_rows = _read_rows(source)
    first_rows: dict[tuple[str, str, str, str], CategoryStoryMappingRow] = {}
    rows: list[CategoryStoryMappingRow] = []
    duplicates: list[DuplicateCategoryStoryMapping] = []
    for row in raw_rows:
        previous = first_rows.get(row.assignment_key)
        if previous is not None:
            duplicates.append(
                DuplicateCategoryStoryMapping(
                    duplicate_row_number=row.source_row_number,
                    first_row_number=previous.source_row_number,
                    assignment_key=row.assignment_key,
                )
            )
            continue
        first_rows[row.assignment_key] = row
        rows.append(row)

    lanes_by_key: dict[tuple[str, str, str], list[CategoryStoryMappingRow]] = defaultdict(list)
    for row in rows:
        lanes_by_key[(row.language_code, row.normalized_category_name, row.canonical_type)].append(row)
    lanes = tuple(
        CategoryStoryLanePlan(
            language_code=language,
            category_name=lane_rows[0].category_name,
            normalized_category_name=category_key,
            canonical_type=canonical_type,
            rows=tuple(lane_rows),
        )
        for (language, category_key, canonical_type), lane_rows in lanes_by_key.items()
    )

    content_candidates: dict[tuple[str, str, str], list[CategoryStoryMappingRow]] = defaultdict(list)
    for row in rows:
        content_candidates[row.content_key].append(row)
    ambiguity_risks = []
    for (language, canonical_type, title_key), candidate_rows in content_candidates.items():
        category_names = tuple(sorted({row.category_name for row in candidate_rows}))
        if len(category_names) < 2:
            continue
        ambiguity_risks.append(
            CategoryStoryAmbiguityRisk(
                language_code=language,
                canonical_type=canonical_type,
                normalized_story_title=title_key,
                category_names=category_names,
                source_row_numbers=tuple(row.source_row_number for row in candidate_rows),
            )
        )

    return CategoryStoryMappingPlan(
        csv_path=str(source),
        source_fingerprint=sha256_file(source),
        raw_row_count=len(raw_rows),
        rows=tuple(rows),
        duplicates=tuple(duplicates),
        lanes=lanes,
        ambiguity_risks=tuple(ambiguity_risks),
    )


def build_mapping_plan(csv_path: str | Path) -> CategoryStoryMappingPlan:
    """Compatibility-friendly alias for callers that use the shorter workflow name."""
    return build_category_story_mapping_plan(csv_path)


def assert_source_unchanged(plan: CategoryStoryMappingPlan) -> None:
    if sha256_file(plan.csv_path) != plan.source_fingerprint:
        raise CategoryStoryMappingValidationError(
            "Category-story mapping CSV changed after preview; rebuild the plan before importing"
        )


def normalize_text(value: object) -> str:
    return unicodedata.normalize("NFC", "" if value is None else str(value)).strip()


def normalize_key(value: object) -> str:
    return normalize_text(value).casefold()


def sha256_file(path: str | Path) -> str:
    digest = hashlib.sha256()
    with Path(path).open("rb") as source:
        for block in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def _read_rows(path: Path) -> list[CategoryStoryMappingRow]:
    try:
        with path.open("r", encoding="utf-8-sig", newline="") as source:
            reader = csv.reader(source, strict=True)
            try:
                header = tuple(normalize_key(value) for value in next(reader))
            except StopIteration as exception:
                raise CategoryStoryMappingValidationError("Category-story mapping CSV is empty") from exception
            if header != EXPECTED_HEADER:
                raise CategoryStoryMappingValidationError(
                    "Category-story mapping CSV must have positional header "
                    "language,type,name,name"
                )

            rows: list[CategoryStoryMappingRow] = []
            for source_order, values in enumerate(reader):
                line_number = reader.line_num
                if len(values) != 4:
                    raise CategoryStoryMappingValidationError(
                        f"Source row {source_order + 1} (line {line_number}) must contain four positional columns"
                    )
                if not values or all(not normalize_text(value) for value in values):
                    raise CategoryStoryMappingValidationError(f"Line {line_number}: blank row is not allowed")

                language = normalize_key(values[0])
                source_type = normalize_text(values[1]).upper()
                category_name = normalize_text(values[2])
                story_title = normalize_text(values[3])
                if language not in SUPPORTED_LANGUAGES:
                    raise CategoryStoryMappingValidationError(
                        f"Line {line_number}: unsupported language {language!r}"
                    )
                if source_type not in SUPPORTED_SOURCE_TYPES:
                    raise CategoryStoryMappingValidationError(
                        f"Line {line_number}: unsupported type {source_type!r}"
                    )
                if not category_name:
                    raise CategoryStoryMappingValidationError(
                        f"Line {line_number}: category name must not be blank"
                    )
                if not story_title:
                    raise CategoryStoryMappingValidationError(
                        f"Line {line_number}: story title must not be blank"
                    )
                rows.append(
                    CategoryStoryMappingRow(
                        source_row_number=source_order + 2,
                        source_order=source_order,
                        language_code=language,
                        source_type=source_type,
                        canonical_type=CANONICAL_TYPE_BY_SOURCE.get(source_type, source_type),
                        category_name=category_name,
                        story_title=story_title,
                        normalized_category_name=normalize_key(category_name),
                        normalized_story_title=normalize_key(story_title),
                    )
                )
            return rows
    except csv.Error as exception:
        raise CategoryStoryMappingValidationError(
            f"Category-story mapping CSV is malformed: {exception}"
        ) from exception


__all__ = [
    "CANONICAL_TYPE_BY_SOURCE",
    "CategoryStoryAmbiguityRisk",
    "CategoryStoryLanePlan",
    "CategoryStoryMappingPlan",
    "CategoryStoryMappingRow",
    "CategoryStoryMappingValidationError",
    "DuplicateCategoryStoryMapping",
    "assert_source_unchanged",
    "build_category_story_mapping_plan",
    "build_mapping_plan",
    "normalize_key",
    "normalize_text",
    "sha256_file",
]
