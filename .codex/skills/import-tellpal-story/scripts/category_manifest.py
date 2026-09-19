from __future__ import annotations

import csv
import hashlib
import json
import re
import unicodedata
from dataclasses import dataclass
from pathlib import Path
from pathlib import PurePosixPath
from urllib.error import HTTPError, URLError
from urllib.parse import unquote, urlparse
from urllib.request import Request, urlopen


SUPPORTED_LANGUAGES = {"tr", "en", "pt", "de"}
SUPPORTED_SOURCE_TYPES = {"STORY", "MEDITATION", "LULLABY", "AUDIO_STORY"}
CANONICAL_TYPES = {"STORY", "MEDITATION", "LULLABY"}
REQUIRED_COLUMNS = {"id", "name", "description", "language", "type", "image_url"}
DEFAULT_MAPPING_PATH = Path(__file__).resolve().parents[1] / "references" / "category_import_mapping.json"
ALLOWED_IMAGE_HOSTS = {"firebasestorage.googleapis.com"}
AUDIO_STORY_SOURCE_IDS = {29, 31, 38, 163}
IMAGE_MIME_TYPES = {
    ".gif": "image/gif",
    ".jpeg": "image/jpeg",
    ".jpg": "image/jpeg",
    ".png": "image/png",
}


class CategoryValidationError(ValueError):
    """Raised when the category CSV or its approved mapping is unsafe to import."""


@dataclass(frozen=True)
class CategoryLocalizationPlan:
    source_id: int
    line_number: int
    language_code: str
    name: str
    description: str | None
    source_type: str
    canonical_type: str
    source_image_url: str
    image_object_path: str
    image_key: str


@dataclass(frozen=True)
class CategoryGroupPlan:
    slug: str
    canonical_type: str
    source_ids: tuple[int, ...]
    localizations: tuple[CategoryLocalizationPlan, ...]


@dataclass(frozen=True)
class CategoryPlan:
    csv_path: str
    mapping_path: str
    source_fingerprint: str
    groups: tuple[CategoryGroupPlan, ...]

    @property
    def row_count(self) -> int:
        return sum(len(group.localizations) for group in self.groups)

    @property
    def image_keys(self) -> tuple[str, ...]:
        return tuple(sorted({item.image_key for group in self.groups for item in group.localizations}))

    def to_dict(self) -> dict[str, object]:
        """Return a report-safe representation without signed URLs or raw storage paths."""
        return {
            "csvPath": self.csv_path,
            "mappingPath": self.mapping_path,
            "sourceFingerprint": self.source_fingerprint,
            "rowCount": self.row_count,
            "groupCount": len(self.groups),
            "imageCount": len(self.image_keys),
            "groups": [
                {
                    "slug": group.slug,
                    "canonicalType": group.canonical_type,
                    "sourceIds": list(group.source_ids),
                    "localizations": [
                        {
                            "sourceId": item.source_id,
                            "lineNumber": item.line_number,
                            "languageCode": item.language_code,
                            "name": item.name,
                            "description": item.description,
                            "sourceType": item.source_type,
                            "canonicalType": item.canonical_type,
                            "imageKey": item.image_key,
                        }
                        for item in group.localizations
                    ],
                }
                for group in self.groups
            ],
        }


def build_category_plan(
    csv_path: str | Path,
    *,
    mapping_path: str | Path | None = None,
) -> CategoryPlan:
    source = Path(csv_path).expanduser().resolve()
    if not source.is_file():
        raise CategoryValidationError(f"Category CSV does not exist: {source}")
    mapping = Path(mapping_path or DEFAULT_MAPPING_PATH).expanduser().resolve()
    groups = _load_mapping(mapping)
    rows = _read_rows(source)
    rows_by_id = {row["id"]: row for row in rows}
    expected_ids = [source_id for group in groups for source_id in group["source_ids"]]
    if len(expected_ids) != len(set(expected_ids)):
        raise CategoryValidationError("Approved category mapping contains duplicate source IDs")
    missing_ids = sorted(set(expected_ids) - set(rows_by_id))
    unexpected_ids = sorted(set(rows_by_id) - set(expected_ids))
    if missing_ids or unexpected_ids:
        raise CategoryValidationError(
            f"CSV and approved mapping differ; missing IDs={missing_ids}, unexpected IDs={unexpected_ids}"
        )

    planned_groups: list[CategoryGroupPlan] = []
    for group in groups:
        localizations: list[CategoryLocalizationPlan] = []
        seen_languages: set[str] = set()
        for source_id in group["source_ids"]:
            row = rows_by_id[source_id]
            source_type = row["type"]
            canonical_type = "STORY" if source_type == "AUDIO_STORY" else source_type
            if canonical_type != group["canonical_type"]:
                raise CategoryValidationError(
                    f"Line {row['line_number']}: source ID {source_id} maps to {group['slug']} "
                    f"with incompatible canonical type {canonical_type}"
                )
            if source_type == "AUDIO_STORY" and group["slug"] != "audio-books":
                raise CategoryValidationError(
                    f"Line {row['line_number']}: AUDIO_STORY source ID {source_id} must map to audio-books"
                )
            if source_type == "AUDIO_STORY" and source_id not in AUDIO_STORY_SOURCE_IDS:
                raise CategoryValidationError(
                    f"Line {row['line_number']}: AUDIO_STORY source ID {source_id} is not approved"
                )
            language = row["language"]
            if language in seen_languages:
                raise CategoryValidationError(
                    f"Group {group['slug']} has duplicate language localization: {language}"
                )
            seen_languages.add(language)
            object_path = parse_image_object_path(row["image_url"], row["line_number"])
            if PurePosixPath(object_path).suffix.casefold() not in IMAGE_MIME_TYPES:
                raise CategoryValidationError(
                    f"Line {row['line_number']}: unsupported category image extension"
                )
            localizations.append(
                CategoryLocalizationPlan(
                    source_id=source_id,
                    line_number=row["line_number"],
                    language_code=language,
                    name=row["name"],
                    description=row["description"],
                    source_type=source_type,
                    canonical_type=canonical_type,
                    source_image_url=_clean_image_url(row["image_url"]),
                    image_object_path=object_path,
                    image_key=hashlib.sha256(object_path.encode("utf-8")).hexdigest(),
                )
            )
        planned_groups.append(
            CategoryGroupPlan(
                slug=group["slug"],
                canonical_type=group["canonical_type"],
                source_ids=tuple(group["source_ids"]),
                localizations=tuple(localizations),
            )
        )
    return CategoryPlan(
        csv_path=str(source),
        mapping_path=str(mapping),
        source_fingerprint=sha256_file(source),
        groups=tuple(planned_groups),
    )


def assert_source_unchanged(plan: CategoryPlan) -> None:
    if sha256_file(plan.csv_path) != plan.source_fingerprint:
        raise CategoryValidationError("Category CSV changed after preview; rebuild the plan before importing")


def verify_source_images(plan: CategoryPlan, *, timeout_seconds: float = 120) -> None:
    """Verify each unique signed image URL before any Admin API mutation."""
    if timeout_seconds <= 0:
        raise CategoryValidationError("Image verification timeout must be positive")
    checked: set[str] = set()
    for group in plan.groups:
        for source in group.localizations:
            if source.image_key in checked:
                continue
            request = Request(
                source.source_image_url,
                headers={"Range": "bytes=0-0", "User-Agent": "TellPalCategoryImport/1.0"},
                method="GET",
            )
            try:
                with urlopen(request, timeout=timeout_seconds) as response:
                    response.read(1)
            except HTTPError as exception:
                raise CategoryValidationError(
                    f"Image source unavailable for source ID {source.source_id} (HTTP {exception.code})"
                ) from exception
            except (OSError, TimeoutError, URLError) as exception:
                raise CategoryValidationError(
                    f"Image source could not be reached for source ID {source.source_id}: "
                    f"{type(exception).__name__}"
                ) from exception
            checked.add(source.image_key)


def parse_image_object_path(image_url: str, line_number: int = 0) -> str:
    value = _clean_image_url(image_url)
    parsed = urlparse(value)
    if parsed.scheme not in {"http", "https"} or not parsed.netloc:
        raise CategoryValidationError(_line_prefix(line_number) + "image_url must be an absolute HTTP(S) URL")
    if parsed.hostname not in ALLOWED_IMAGE_HOSTS:
        raise CategoryValidationError(_line_prefix(line_number) + "image_url host is not an approved Firebase Storage host")
    match = re.fullmatch(r"/v0/b/[^/]+/o/(.+)", parsed.path)
    if not match:
        raise CategoryValidationError(_line_prefix(line_number) + "image_url does not contain a Firebase object path")
    object_path = unquote(match.group(1)).strip()
    if not object_path or object_path.startswith("/") or "\\" in object_path:
        raise CategoryValidationError(_line_prefix(line_number) + "Firebase object path is invalid")
    if any(part in {"", ".", ".."} for part in object_path.split("/")):
        raise CategoryValidationError(_line_prefix(line_number) + "Firebase object path contains an unsafe segment")
    return object_path


def _clean_image_url(image_url: object) -> str:
    value = normalize_text(image_url)
    if len(value) >= 2 and value[0] == value[-1] == '"':
        value = value[1:-1].strip()
    return value


def normalize_text(value: object) -> str:
    return unicodedata.normalize("NFC", "" if value is None else str(value)).strip()


def normalize_optional_text(value: object) -> str | None:
    normalized = normalize_text(value)
    return normalized or None


def sha256_file(path: str | Path) -> str:
    digest = hashlib.sha256()
    with Path(path).open("rb") as source:
        for block in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def _read_rows(path: Path) -> list[dict[str, object]]:
    try:
        with path.open("r", encoding="utf-8-sig", newline="") as source:
            reader = csv.reader(source, strict=True)
            try:
                headers = [normalize_text(item) for item in next(reader)]
            except StopIteration as exception:
                raise CategoryValidationError("Category CSV is empty") from exception
            if len(headers) != len(set(headers)):
                raise CategoryValidationError("Category CSV has duplicate columns")
            missing = sorted(REQUIRED_COLUMNS - set(headers))
            unexpected = sorted(set(headers) - REQUIRED_COLUMNS)
            if missing or unexpected:
                raise CategoryValidationError(
                    f"Category CSV columns mismatch; missing={missing}, unexpected={unexpected}"
                )
            rows: list[dict[str, object]] = []
            seen_ids: set[int] = set()
            for line_number, values in enumerate(reader, start=2):
                if not values or all(not normalize_text(value) for value in values):
                    raise CategoryValidationError(f"Line {line_number}: blank row is not allowed")
                if len(values) != len(headers):
                    raise CategoryValidationError(f"Line {line_number}: column count does not match the header")
                raw = dict(zip(headers, values))
                try:
                    source_id = int(normalize_text(raw["id"]))
                except ValueError as exception:
                    raise CategoryValidationError(f"Line {line_number}: id must be a positive integer") from exception
                if source_id <= 0 or source_id in seen_ids:
                    raise CategoryValidationError(f"Line {line_number}: id must be unique and positive")
                language = normalize_text(raw["language"]).casefold()
                source_type = normalize_text(raw["type"]).upper()
                if language not in SUPPORTED_LANGUAGES:
                    raise CategoryValidationError(f"Line {line_number}: unsupported language {language!r}")
                if source_type not in SUPPORTED_SOURCE_TYPES:
                    raise CategoryValidationError(f"Line {line_number}: unsupported type {source_type!r}")
                name = normalize_text(raw["name"])
                if not name:
                    raise CategoryValidationError(f"Line {line_number}: name must not be blank")
                description = normalize_optional_text(raw["description"])
                image_url = normalize_text(raw["image_url"])
                if not image_url:
                    raise CategoryValidationError(f"Line {line_number}: image_url must not be blank")
                seen_ids.add(source_id)
                rows.append(
                    {
                        "id": source_id,
                        "line_number": line_number,
                        "name": name,
                        "description": description,
                        "language": language,
                        "type": source_type,
                        "image_url": image_url,
                    }
                )
            return rows
    except csv.Error as exception:
        raise CategoryValidationError(f"Category CSV is malformed: {exception}") from exception


def _load_mapping(path: Path) -> list[dict[str, object]]:
    if not path.is_file():
        raise CategoryValidationError(f"Category mapping does not exist: {path}")
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exception:
        raise CategoryValidationError(f"Category mapping cannot be read: {path}") from exception
    raw_groups = payload.get("groups") if isinstance(payload, dict) else None
    if not isinstance(raw_groups, list) or not raw_groups:
        raise CategoryValidationError("Category mapping must contain a non-empty groups list")
    result: list[dict[str, object]] = []
    seen_slugs: set[str] = set()
    for item in raw_groups:
        if not isinstance(item, dict):
            raise CategoryValidationError("Category mapping group must be an object")
        slug = normalize_text(item.get("slug"))
        canonical_type = normalize_text(item.get("canonicalType")).upper()
        source_ids = item.get("sourceIds")
        if not slug or slug in seen_slugs or not re.fullmatch(r"[a-z0-9]+(?:-[a-z0-9]+)*", slug):
            raise CategoryValidationError(f"Invalid or duplicate category slug in mapping: {slug!r}")
        if canonical_type not in CANONICAL_TYPES:
            raise CategoryValidationError(f"Unsupported canonical type in mapping: {canonical_type!r}")
        if (
            not isinstance(source_ids, list)
            or not source_ids
            or any(isinstance(value, bool) or not isinstance(value, int) for value in source_ids)
        ):
            raise CategoryValidationError(f"Category mapping group {slug} must have positive integer source IDs")
        if any(value <= 0 for value in source_ids):
            raise CategoryValidationError(f"Category mapping group {slug} contains a non-positive source ID")
        seen_slugs.add(slug)
        result.append({"slug": slug, "canonical_type": canonical_type, "source_ids": tuple(source_ids)})
    return result


def _line_prefix(line_number: int) -> str:
    return f"Line {line_number}: " if line_number else ""
