from __future__ import annotations

import csv
import hashlib
import json
import re
import unicodedata
import xml.etree.ElementTree as ElementTree
import zipfile
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Callable, Iterable


SUPPORTED_LANGUAGES = ("tr", "en", "es", "pt", "de")
IMAGE_SUFFIXES = {".jpg", ".jpeg", ".png", ".gif"}
TEXTLESS_DIRECTORY_NAMES = {"yazısız", "yazisiz"}
REQUIRED_METADATA_COLUMNS = {
    "id",
    "is_publish",
    "name",
    "summary",
    "page_count",
    "author",
    "dubbing",
    "illustrator",
    "duration",
    "age_range",
}


class StoryValidationError(ValueError):
    """Raised before remote writes when a prepared story folder is invalid."""


@dataclass(frozen=True)
class MetadataRow:
    legacy_id: str
    is_publish: bool
    name: str
    summary: str | None
    page_count: int
    author: str
    narrator: str
    illustrator: str
    duration_minutes: int
    age_range: int


@dataclass(frozen=True)
class StoryPagePlan:
    page_number: int
    body_text: str
    illustration_path: str
    audio_path: str


@dataclass(frozen=True)
class StoryLocalizationPlan:
    language_code: str
    title: str
    description: str | None
    duration_minutes: int
    is_publish: bool
    cover_path: str
    pages: tuple[StoryPagePlan, ...]
    author: str
    narrator: str
    illustrator: str


@dataclass(frozen=True)
class ContributorAssignmentPlan:
    display_name: str
    role: str
    language_code: str | None


@dataclass(frozen=True)
class FingerprintedFile:
    path: str
    byte_size: int
    checksum_sha256: str


@dataclass(frozen=True)
class StoryPlan:
    story_directory: str
    external_key: str
    age_range: int
    active: bool
    page_count: int
    localizations: tuple[StoryLocalizationPlan, ...]
    textless_cover_path: str | None
    textless_page_paths: tuple[str, ...]
    contributor_assignments: tuple[ContributorAssignmentPlan, ...]
    warnings: tuple[str, ...]
    fingerprint: str
    fingerprinted_files: tuple[FingerprintedFile, ...]
    expected_actions: dict[str, int]

    def to_dict(self) -> dict[str, object]:
        return asdict(self)

    def resolve_path(self, relative_path: str) -> Path:
        return Path(self.story_directory, *relative_path.split("/"))

    def checksum_for(self, relative_path: str) -> str:
        for item in self.fingerprinted_files:
            if item.path == relative_path:
                return item.checksum_sha256
        raise StoryValidationError(f"Fingerprint entry is missing for {relative_path}")

    def size_for(self, relative_path: str) -> int:
        for item in self.fingerprinted_files:
            if item.path == relative_path:
                return item.byte_size
        raise StoryValidationError(f"Fingerprint entry is missing for {relative_path}")


@dataclass(frozen=True)
class LanguageInventory:
    docx_path: Path
    cover_path: Path
    illustrations: dict[int, Path]
    audio_files: dict[int, Path]
    warnings: tuple[str, ...]


@dataclass(frozen=True)
class TextlessInventory:
    cover_path: Path
    illustrations: dict[int, Path]
    warnings: tuple[str, ...]


def build_story_plan(
    story_directory: str | Path,
    *,
    textless_page_overrides: dict[int, str] | None = None,
    metadata_row_overrides: dict[str, str] | None = None,
    description_overrides: dict[str, str | None] | None = None,
    allow_missing_textless: bool = False,
) -> StoryPlan:
    root = Path(story_directory).expanduser().resolve()
    if not root.is_dir():
        raise StoryValidationError(f"Story directory does not exist: {root}")

    metadata_path = _find_metadata_file(root)
    metadata_rows = _read_metadata(metadata_path)
    page_count = _single_value(metadata_rows, "page_count")
    age_range = _single_value(metadata_rows, "age_range")
    language_directories, textless_directory = _discover_directories(
        root,
        require_textless=not allow_missing_textless,
    )
    if allow_missing_textless and textless_page_overrides:
        raise StoryValidationError(
            "--allow-missing-textless cannot be combined with textless page overrides"
        )
    row_overrides = metadata_row_overrides or {}
    _validate_metadata_row_overrides(row_overrides, language_directories, metadata_rows)
    localization_description_overrides = description_overrides or {}
    unknown_description_languages = sorted(
        set(localization_description_overrides) - set(language_directories)
    )
    if unknown_description_languages:
        raise StoryValidationError(
            "Description overrides reference unavailable languages: "
            f"{unknown_description_languages}"
        )

    if len(language_directories) != len(metadata_rows):
        raise StoryValidationError(
            "metadata.csv row count must equal the number of language directories "
            f"({len(metadata_rows)} rows, {len(language_directories)} directories)"
        )

    inventories: dict[str, LanguageInventory] = {}
    rows_by_language: dict[str, MetadataRow] = {}
    page_texts_by_language: dict[str, dict[int, str]] = {}
    warnings: list[str] = []
    matched_row_ids: set[str] = set()
    unresolved_languages: list[tuple[str, LanguageInventory, dict[int, str], str]] = []

    metadata_by_title: dict[str, list[MetadataRow]] = {}
    for row in metadata_rows:
        metadata_by_title.setdefault(normalize_key(row.name), []).append(row)

    for language_code, directory in language_directories.items():
        inventory = _inventory_language_directory(root, directory, page_count)
        if language_code in row_overrides:
            row = next(row for row in metadata_rows if row.legacy_id == row_overrides[language_code])
            page_texts = _extract_pages_for_explicit_metadata_row(inventory.docx_path, row, page_count)
            matched_row_ids.add(row.legacy_id)
            inventories[language_code] = inventory
            rows_by_language[language_code] = row
            page_texts_by_language[language_code] = page_texts
            warnings.extend(inventory.warnings)
            warnings.append(
                f"Explicit metadata row override: {language_code} uses legacy id {row.legacy_id} "
                f"with title {row.name!r}"
            )
            continue
        try:
            docx_title, page_texts = extract_docx_pages(inventory.docx_path, page_count)
        except StoryValidationError:
            docx_title = _first_docx_text(inventory.docx_path)
            if normalize_key(docx_title) in metadata_by_title:
                raise
            page_texts = _extract_docx_pages_without_title(inventory.docx_path, page_count)
            if page_texts is None:
                raise
        candidates = [
            row
            for row in metadata_by_title.get(normalize_key(docx_title), [])
            if row.legacy_id not in matched_row_ids
        ]
        if len(candidates) > 1:
            raise StoryValidationError(
                f"DOCX title in {relative_path(root, inventory.docx_path)} must match exactly one "
                f"metadata name; found {len(candidates)} matches for {docx_title!r}"
            )
        if not candidates:
            no_title_pages = _extract_docx_pages_without_title(inventory.docx_path, page_count)
            if no_title_pages is None:
                raise StoryValidationError(
                    f"DOCX title in {relative_path(root, inventory.docx_path)} must match exactly one "
                    f"metadata name; found 0 matches for {docx_title!r}"
                )
            filename_candidates = [
                row
                for row in metadata_rows
                if row.legacy_id not in matched_row_ids
                and _metadata_title_in_docx_filename(row.name, inventory.docx_path)
            ]
            if len(filename_candidates) > 1:
                raise StoryValidationError(
                    f"DOCX filename {inventory.docx_path.name!r} matches multiple metadata names: "
                    f"{[row.name for row in filename_candidates]}"
                )
            if filename_candidates:
                row = filename_candidates[0]
                matched_row_ids.add(row.legacy_id)
                inventories[language_code] = inventory
                rows_by_language[language_code] = row
                page_texts_by_language[language_code] = no_title_pages
                warnings.extend(inventory.warnings)
                warnings.append(
                    f"Matched {relative_path(root, inventory.docx_path)} to metadata {row.name!r} "
                    "by its unique DOCX filename"
                )
                continue
            inventories[language_code] = inventory
            unresolved_languages.append((language_code, inventory, no_title_pages, docx_title))
            warnings.extend(inventory.warnings)
            continue
        row = candidates[0]
        if row.legacy_id in matched_row_ids:
            raise StoryValidationError(f"Metadata row matched more than once: {row.name}")
        matched_row_ids.add(row.legacy_id)
        inventories[language_code] = inventory
        rows_by_language[language_code] = row
        page_texts_by_language[language_code] = page_texts
        warnings.extend(inventory.warnings)

    remaining_rows = [row for row in metadata_rows if row.legacy_id not in matched_row_ids]
    if unresolved_languages:
        if len(unresolved_languages) != 1 or len(remaining_rows) != 1:
            unresolved_paths = [relative_path(root, item[1].docx_path) for item in unresolved_languages]
            remaining_names = [row.name for row in remaining_rows]
            raise StoryValidationError(
                "Cannot safely infer metadata rows for DOCX files without matching titles; "
                f"documents={unresolved_paths}, metadata={remaining_names}"
            )
        language_code, inventory, page_texts, observed_title = unresolved_languages[0]
        row = remaining_rows[0]
        matched_row_ids.add(row.legacy_id)
        rows_by_language[language_code] = row
        page_texts_by_language[language_code] = page_texts
        warnings.append(
            f"Matched {relative_path(root, inventory.docx_path)} to metadata {row.name!r} as the sole "
            f"remaining row; first DOCX text was {observed_title!r}"
        )

    if len(matched_row_ids) != len(metadata_rows):
        unmatched = [row.name for row in metadata_rows if row.legacy_id not in matched_row_ids]
        raise StoryValidationError(f"Metadata rows without a matching language DOCX: {unmatched}")

    localizations: list[StoryLocalizationPlan] = []
    language_by_row_id = {row.legacy_id: code for code, row in rows_by_language.items()}
    for row in metadata_rows:
        language_code = language_by_row_id[row.legacy_id]
        inventory = inventories[language_code]
        page_texts = page_texts_by_language[language_code]
        pages = tuple(
            StoryPagePlan(
                page_number=page_number,
                body_text=page_texts[page_number],
                illustration_path=relative_path(root, inventory.illustrations[page_number]),
                audio_path=relative_path(root, inventory.audio_files[page_number]),
            )
            for page_number in range(1, page_count + 1)
        )
        localizations.append(
            StoryLocalizationPlan(
                language_code=language_code,
                title=row.name,
                description=localization_description_overrides.get(language_code, row.summary),
                duration_minutes=row.duration_minutes,
                is_publish=row.is_publish,
                cover_path=relative_path(root, inventory.cover_path),
                pages=pages,
                author=row.author,
                narrator=row.narrator,
                illustrator=row.illustrator,
            )
        )
        if language_code in localization_description_overrides:
            warnings.append(
                f"Explicit description override: {language_code} uses "
                f"{localization_description_overrides[language_code]!r}"
            )

    if "tr" not in rows_by_language:
        raise StoryValidationError("A tr language directory and matching Turkish metadata row are required")
    external_key = f"story.{slugify_turkish_title(rows_by_language['tr'].name)}"

    textless: TextlessInventory | None = None
    if allow_missing_textless:
        if textless_directory is not None and any(textless_directory.iterdir()):
            raise StoryValidationError(
                "--allow-missing-textless is allowed only when the yazısız/yazisiz directory "
                "is empty; remove the option or complete the textless media set"
            )
        warnings.append(
            "Explicitly allowed missing textless media: no textless cover or page images will be attached"
        )
    else:
        if textless_directory is None:
            raise StoryValidationError("A yazısız/yazisiz directory is required")
        textless = _inventory_textless_directory(
            root,
            textless_directory,
            page_count,
            textless_page_overrides or {},
        )
        warnings.extend(textless.warnings)
    contributors = _build_contributor_assignments(localizations)
    fingerprinted_files, fingerprint = fingerprint_directory(root)
    publication_count = sum(localization.is_publish for localization in localizations)
    textless_media_uploads = 0 if textless is None else 1 + page_count
    media_uploads = len(localizations) * (1 + page_count * 2) + textless_media_uploads
    textless_mutations = 0 if textless is None else 1 + page_count
    fixed_mutations = (
        1
        + media_uploads
        + len(localizations)
        + page_count
        + len(localizations) * page_count
        + textless_mutations
        + len(contributors)
        + publication_count
    )

    return StoryPlan(
        story_directory=str(root),
        external_key=external_key,
        age_range=age_range,
        active=publication_count > 0,
        page_count=page_count,
        localizations=tuple(localizations),
        textless_cover_path=(relative_path(root, textless.cover_path) if textless is not None else None),
        textless_page_paths=(
            tuple(
                relative_path(root, textless.illustrations[number])
                for number in range(1, page_count + 1)
            )
            if textless is not None
            else ()
        ),
        contributor_assignments=contributors,
        warnings=tuple(sorted(set(warnings))),
        fingerprint=fingerprint,
        fingerprinted_files=fingerprinted_files,
        expected_actions={
            "media_uploads": media_uploads,
            "content_localizations": len(localizations),
            "story_pages": page_count,
            "page_localizations": len(localizations) * page_count,
            "contributor_assignments": len(contributors),
            "publications": publication_count,
            "fixed_mutations_excluding_contributor_creates": fixed_mutations,
        },
    )


def extract_docx_pages(docx_path: str | Path, page_count: int) -> tuple[str, dict[int, str]]:
    path = Path(docx_path)
    try:
        paragraphs = [normalize_text(paragraph, keep_empty=True) for paragraph in _read_docx_paragraphs(path)]
    except Exception as exception:
        raise StoryValidationError(f"Cannot read DOCX {path}: {exception}") from exception

    title_index = next((index for index, value in enumerate(paragraphs) if value), None)
    if title_index is None:
        raise StoryValidationError(f"DOCX contains no text: {path}")
    title = paragraphs[title_index]
    page_paragraphs = paragraphs[title_index + 1 :]
    page_texts, marker_error = _extract_marked_pages(path, page_paragraphs, page_count)
    missing_pages = [number for number in range(1, page_count + 1) if number not in page_texts]
    if marker_error or missing_pages:
        blank_separated_pages = _extract_blank_separated_pages(path, page_paragraphs, page_count)
        if blank_separated_pages is not None:
            return title, blank_separated_pages
        if marker_error:
            raise StoryValidationError(marker_error)
        raise StoryValidationError(f"DOCX {path.name} is missing page markers: {missing_pages}")
    return title, page_texts


def _extract_marked_pages(
    path: Path,
    paragraphs: list[str],
    page_count: int,
) -> tuple[dict[int, str], str | None]:
    page_texts: dict[int, str] = {}
    current_page = 0
    current_lines: list[str] = []
    marker_error: str | None = None

    for paragraph in paragraphs:
        if not paragraph:
            continue
        expected_page = current_page + 1
        marker_body = _page_marker_body(paragraph, expected_page) if expected_page <= page_count else None
        if marker_body is not None:
            if current_page:
                page_texts[current_page] = _finalize_page_text(path, current_page, current_lines)
            current_page = expected_page
            current_lines = [marker_body] if marker_body else []
            continue
        if current_page == 0:
            marker_error = f"Unexpected text before page 1 marker in {path.name}: {paragraph!r}"
            break
        current_lines.append(paragraph)

    if current_page:
        page_texts[current_page] = _finalize_page_text(path, current_page, current_lines)
    return page_texts, marker_error


def _extract_blank_separated_pages(
    path: Path,
    paragraphs: list[str],
    page_count: int,
) -> dict[int, str] | None:
    blocks: list[list[str]] = []
    current: list[str] = []
    for paragraph in paragraphs:
        if paragraph:
            current.append(paragraph)
        elif current:
            blocks.append(current)
            current = []
    if current:
        blocks.append(current)
    if len(blocks) != page_count:
        return None

    pages: dict[int, str] = {}
    for page_number, block in enumerate(blocks, start=1):
        first_body = _page_marker_body(block[0], page_number)
        lines = ([first_body] if first_body else []) + block[1:] if first_body is not None else block
        pages[page_number] = _finalize_page_text(path, page_number, lines)
    return pages


def _extract_docx_pages_without_title(path: Path, page_count: int) -> dict[int, str] | None:
    paragraphs = [
        normalize_text(paragraph, keep_empty=True)
        for paragraph in _read_docx_paragraphs(path)
    ]
    first_content = next((index for index, value in enumerate(paragraphs) if value), None)
    if first_content is None:
        return None
    content_paragraphs = paragraphs[first_content:]
    marked_pages, marker_error = _extract_marked_pages(path, content_paragraphs, page_count)
    if marker_error is None and len(marked_pages) == page_count:
        return marked_pages
    blank_separated_pages = _extract_blank_separated_pages(path, content_paragraphs, page_count)
    if blank_separated_pages is not None:
        return blank_separated_pages
    non_empty_paragraphs = [paragraph for paragraph in content_paragraphs if paragraph]
    if len(non_empty_paragraphs) != page_count:
        return None
    return {
        page_number: _finalize_page_text(path, page_number, [paragraph])
        for page_number, paragraph in enumerate(non_empty_paragraphs, start=1)
    }


def _extract_pages_for_explicit_metadata_row(
    path: Path,
    row: MetadataRow,
    page_count: int,
) -> dict[int, str]:
    try:
        title, pages = extract_docx_pages(path, page_count)
        if normalize_key(title) == normalize_key(row.name):
            return pages
    except StoryValidationError:
        pass
    paragraphs = [
        normalize_text(paragraph, keep_empty=True)
        for paragraph in _read_docx_paragraphs(path)
    ]
    marked_pages, marker_error = _extract_marked_pages(path, paragraphs, page_count)
    if marker_error is None and len(marked_pages) == page_count:
        return marked_pages
    pages = _extract_docx_pages_without_title(path, page_count)
    if pages is None:
        raise StoryValidationError(
            f"DOCX {path.name} selected for metadata id {row.legacy_id} cannot be parsed into "
            f"exactly {page_count} pages"
        )
    return pages


def _first_docx_text(path: Path) -> str:
    paragraphs = [normalize_text(paragraph, keep_empty=True) for paragraph in _read_docx_paragraphs(path)]
    first = next((value for value in paragraphs if value), None)
    if first is None:
        raise StoryValidationError(f"DOCX contains no text: {path}")
    return first


def _metadata_title_in_docx_filename(metadata_title: str, docx_path: Path) -> bool:
    title_key = _filename_match_key(metadata_title)
    filename_key = _filename_match_key(docx_path.stem)
    return bool(title_key) and f" {title_key} " in f" {filename_key} "


def _filename_match_key(value: str) -> str:
    decomposed = unicodedata.normalize("NFKD", normalize_text(value).casefold())
    without_marks = "".join(character for character in decomposed if not unicodedata.combining(character))
    return " ".join(re.findall(r"[^\W_]+", without_marks, flags=re.UNICODE))


def _read_docx_paragraphs(path: Path) -> list[str]:
    word_namespace = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
    qualified = lambda name: f"{{{word_namespace}}}{name}"
    with zipfile.ZipFile(path) as archive:
        document_xml = archive.read("word/document.xml")
        numbering_xml = archive.read("word/numbering.xml") if "word/numbering.xml" in archive.namelist() else None
    document = ElementTree.fromstring(document_xml)
    numbering_starts = _docx_numbering_starts(numbering_xml, qualified)
    numbering_counters: dict[tuple[str, str], int] = {}
    body = document.find(f".//{qualified('body')}")
    if body is None:
        raise StoryValidationError(f"DOCX has no Word document body: {path}")
    paragraphs: list[str] = []
    for paragraph in body.iter(qualified("p")):
        parts: list[str] = []
        for node in paragraph.iter():
            if node.tag == qualified("t"):
                parts.append(node.text or "")
            elif node.tag == qualified("tab"):
                parts.append("\t")
            elif node.tag in {qualified("br"), qualified("cr")}:
                parts.append("\n")
        paragraph_text = "".join(parts)
        numbering = paragraph.find(f"./{qualified('pPr')}/{qualified('numPr')}")
        if numbering is not None:
            num_id_node = numbering.find(f"./{qualified('numId')}")
            level_node = numbering.find(f"./{qualified('ilvl')}")
            num_id = num_id_node.get(qualified("val")) if num_id_node is not None else None
            level = level_node.get(qualified("val"), "0") if level_node is not None else "0"
            key = (num_id, level) if num_id is not None else None
            if key in numbering_starts:
                number = numbering_counters.get(key, numbering_starts[key])
                numbering_counters[key] = number + 1
                paragraph_text = f"{number}. {paragraph_text}"
        paragraphs.append(paragraph_text)
    return paragraphs


def _docx_numbering_starts(
    numbering_xml: bytes | None,
    qualified: Callable[[str], str],
) -> dict[tuple[str, str], int]:
    if numbering_xml is None:
        return {}
    numbering = ElementTree.fromstring(numbering_xml)
    abstract_levels: dict[tuple[str, str], int] = {}
    for abstract in numbering.findall(f"./{qualified('abstractNum')}"):
        abstract_id = abstract.get(qualified("abstractNumId"))
        for level in abstract.findall(f"./{qualified('lvl')}"):
            level_id = level.get(qualified("ilvl"), "0")
            number_format = level.find(f"./{qualified('numFmt')}")
            start = level.find(f"./{qualified('start')}")
            if (
                abstract_id is not None
                and number_format is not None
                and number_format.get(qualified("val")) == "decimal"
                and start is not None
            ):
                abstract_levels[(abstract_id, level_id)] = int(start.get(qualified("val"), "1"))

    starts: dict[tuple[str, str], int] = {}
    for instance in numbering.findall(f"./{qualified('num')}"):
        num_id = instance.get(qualified("numId"))
        abstract_node = instance.find(f"./{qualified('abstractNumId')}")
        abstract_id = abstract_node.get(qualified("val")) if abstract_node is not None else None
        if num_id is None or abstract_id is None:
            continue
        for (candidate_abstract_id, level_id), start in abstract_levels.items():
            if candidate_abstract_id == abstract_id:
                starts[(num_id, level_id)] = start
        for override in instance.findall(f"./{qualified('lvlOverride')}"):
            level_id = override.get(qualified("ilvl"), "0")
            start_override = override.find(f"./{qualified('startOverride')}")
            if start_override is not None:
                starts[(num_id, level_id)] = int(start_override.get(qualified("val"), "1"))
    return starts


def fingerprint_directory(story_directory: str | Path) -> tuple[tuple[FingerprintedFile, ...], str]:
    root = Path(story_directory).resolve()
    entries = tuple(
        FingerprintedFile(
            path=relative_path(root, path),
            byte_size=path.stat().st_size,
            checksum_sha256=sha256_file(path),
        )
        for path in sorted((item for item in root.rglob("*") if item.is_file()), key=lambda item: relative_path(root, item))
    )
    serialized = json.dumps([asdict(entry) for entry in entries], ensure_ascii=False, sort_keys=True).encode("utf-8")
    return entries, hashlib.sha256(serialized).hexdigest()


def sha256_file(path: str | Path) -> str:
    digest = hashlib.sha256()
    with Path(path).open("rb") as source:
        for block in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def slugify_turkish_title(title: str) -> str:
    prepared = normalize_text(title).translate(str.maketrans({"ı": "i", "İ": "I"}))
    ascii_text = unicodedata.normalize("NFKD", prepared).encode("ascii", "ignore").decode("ascii")
    slug = re.sub(r"[^a-z0-9]+", "-", ascii_text.lower()).strip("-")
    if not slug:
        raise StoryValidationError("Turkish title cannot produce a non-empty external-key slug")
    return slug


def normalize_text(value: object, *, keep_empty: bool = False) -> str:
    normalized = unicodedata.normalize("NFC", str(value or "").replace("\r\n", "\n").replace("\r", "\n"))
    normalized = normalized.strip()
    return normalized if normalized or keep_empty else ""


def normalize_key(value: object) -> str:
    return normalize_text(value).casefold()


def relative_path(root: Path, path: Path) -> str:
    return path.resolve().relative_to(root.resolve()).as_posix()


def _find_metadata_file(root: Path) -> Path:
    matches = [path for path in root.iterdir() if path.is_file() and path.name.casefold() == "metadata.csv"]
    if len(matches) != 1:
        raise StoryValidationError(f"Expected exactly one metadata.csv in {root}; found {len(matches)}")
    return matches[0]


def _read_metadata(path: Path) -> tuple[MetadataRow, ...]:
    try:
        with path.open("r", encoding="utf-8-sig", newline="") as source:
            reader = csv.DictReader(source)
            fieldnames = {normalize_text(name) for name in (reader.fieldnames or [])}
            missing = sorted(REQUIRED_METADATA_COLUMNS - fieldnames)
            if missing:
                raise StoryValidationError(f"metadata.csv is missing columns: {missing}")
            rows = tuple(_metadata_row(row, index) for index, row in enumerate(reader, start=2))
    except UnicodeDecodeError as exception:
        raise StoryValidationError("metadata.csv must be UTF-8 encoded") from exception
    if not rows:
        raise StoryValidationError("metadata.csv must contain at least one story localization row")
    duplicate_ids = _duplicates(row.legacy_id for row in rows)
    if duplicate_ids:
        raise StoryValidationError(f"metadata.csv contains duplicate legacy ids: {duplicate_ids}")
    return rows


def _validate_metadata_row_overrides(
    overrides: dict[str, str],
    language_directories: dict[str, Path],
    rows: tuple[MetadataRow, ...],
) -> None:
    unknown_languages = sorted(set(overrides) - set(language_directories))
    if unknown_languages:
        raise StoryValidationError(
            f"Metadata row overrides reference unavailable languages: {unknown_languages}"
        )
    known_ids = {row.legacy_id for row in rows}
    unknown_ids = sorted(set(overrides.values()) - known_ids)
    if unknown_ids:
        raise StoryValidationError(f"Metadata row overrides reference unknown legacy ids: {unknown_ids}")
    duplicate_ids = _duplicates(overrides.values())
    if duplicate_ids:
        raise StoryValidationError(
            f"One metadata legacy id cannot be assigned to multiple languages: {duplicate_ids}"
        )


def _metadata_row(row: dict[str, str], line_number: int) -> MetadataRow:
    def required(name: str) -> str:
        value = normalize_text(row.get(name))
        if not value:
            raise StoryValidationError(f"metadata.csv line {line_number}: {name} is required")
        return value

    return MetadataRow(
        legacy_id=required("id"),
        is_publish=_parse_bool(required("is_publish"), line_number),
        name=required("name"),
        summary=normalize_text(row.get("summary")) or None,
        page_count=_parse_non_negative_int(required("page_count"), "page_count", line_number, positive=True),
        author=required("author"),
        narrator=required("dubbing"),
        illustrator=required("illustrator"),
        duration_minutes=_parse_non_negative_int(required("duration"), "duration", line_number),
        age_range=_parse_non_negative_int(required("age_range"), "age_range", line_number),
    )


def _parse_bool(value: str, line_number: int) -> bool:
    normalized = value.casefold()
    if normalized in {"true", "1", "yes", "y"}:
        return True
    if normalized in {"false", "0", "no", "n"}:
        return False
    raise StoryValidationError(f"metadata.csv line {line_number}: invalid is_publish value {value!r}")


def _parse_non_negative_int(value: str, field: str, line_number: int, *, positive: bool = False) -> int:
    try:
        parsed = int(value)
    except ValueError as exception:
        raise StoryValidationError(f"metadata.csv line {line_number}: {field} must be an integer") from exception
    minimum = 1 if positive else 0
    if parsed < minimum:
        raise StoryValidationError(f"metadata.csv line {line_number}: {field} must be >= {minimum}")
    return parsed


def _single_value(rows: Iterable[MetadataRow], field: str) -> int:
    values = {getattr(row, field) for row in rows}
    if len(values) != 1:
        raise StoryValidationError(f"All metadata rows must have the same {field}; found {sorted(values)}")
    return values.pop()


def _discover_directories(
    root: Path,
    *,
    require_textless: bool = True,
) -> tuple[dict[str, Path], Path | None]:
    languages: dict[str, Path] = {}
    textless: list[Path] = []
    unexpected: list[str] = []
    for directory in (path for path in root.iterdir() if path.is_dir()):
        normalized_name = normalize_key(directory.name)
        if normalized_name in SUPPORTED_LANGUAGES:
            if normalized_name in languages:
                raise StoryValidationError(f"Duplicate language directory: {normalized_name}")
            languages[normalized_name] = directory
        elif normalized_name in TEXTLESS_DIRECTORY_NAMES:
            textless.append(directory)
        elif not directory.name.startswith("."):
            unexpected.append(directory.name)
    if unexpected:
        raise StoryValidationError(f"Unsupported directories in story root: {sorted(unexpected)}")
    if not languages:
        raise StoryValidationError("At least one supported language directory is required")
    expected_count = 1 if require_textless else "zero or one"
    if (require_textless and len(textless) != 1) or (not require_textless and len(textless) > 1):
        raise StoryValidationError(
            f"Expected {expected_count} yazısız/yazisiz directory; found {len(textless)}"
        )
    return languages, textless[0] if textless else None


def _inventory_language_directory(root: Path, directory: Path, page_count: int) -> LanguageInventory:
    if any(path.is_dir() for path in directory.iterdir()):
        raise StoryValidationError(f"Nested directories are not supported inside {directory.name}")
    docx_files: list[Path] = []
    cover_candidates: list[Path] = []
    illustration_candidates: dict[int, list[Path]] = {}
    audio_candidates: dict[int, list[Path]] = {}
    warnings: list[str] = []

    for path in (item for item in directory.iterdir() if item.is_file()):
        suffix = path.suffix.casefold()
        page_number = _leading_page_number(path.stem)
        if suffix == ".docx" and not path.name.startswith("~$"):
            docx_files.append(path)
        elif suffix in IMAGE_SUFFIXES and _is_cover_name(path.stem):
            cover_candidates.append(path)
        elif page_number is not None and suffix in IMAGE_SUFFIXES:
            _add_candidate(illustration_candidates, page_number, path)
        elif page_number is not None and suffix == ".mp3":
            _add_candidate(audio_candidates, page_number, path)
        elif page_number is not None:
            raise StoryValidationError(f"Unsupported numbered media file: {relative_path(root, path)}")
        else:
            warnings.append(
                f"Ignored for upload but included in source fingerprint: {relative_path(root, path)}"
            )

    if len(docx_files) != 1:
        raise StoryValidationError(f"{directory.name} must contain exactly one DOCX; found {len(docx_files)}")
    cover_path, cover_warnings = _select_cover(root, directory.name, cover_candidates)
    illustrations, illustration_warnings = _select_numbered_candidates(
        root, directory.name, "illustration", illustration_candidates
    )
    audio_files, audio_warnings = _select_numbered_candidates(
        root, directory.name, "audio", audio_candidates
    )
    _validate_image_file(cover_path)
    for path in illustrations.values():
        _validate_image_file(path)
    for path in audio_files.values():
        _validate_mp3_file(path)
    warnings.extend(cover_warnings + illustration_warnings + audio_warnings)
    _require_page_numbers(directory.name, "illustrations", illustrations, page_count)
    _require_page_numbers(directory.name, "audio files", audio_files, page_count)
    return LanguageInventory(docx_files[0], cover_path, illustrations, audio_files, tuple(warnings))


def _inventory_textless_directory(
    root: Path,
    directory: Path,
    page_count: int,
    page_overrides: dict[int, str],
) -> TextlessInventory:
    if any(path.is_dir() for path in directory.iterdir()):
        raise StoryValidationError(f"Nested directories are not supported inside {directory.name}")
    cover_candidates: list[Path] = []
    illustration_candidates: dict[int, list[Path]] = {}
    warnings: list[str] = []
    for path in (item for item in directory.iterdir() if item.is_file()):
        suffix = path.suffix.casefold()
        page_number = _leading_page_number(path.stem)
        if suffix in IMAGE_SUFFIXES and _is_cover_name(path.stem):
            cover_candidates.append(path)
        elif page_number is not None and suffix in IMAGE_SUFFIXES:
            _add_candidate(illustration_candidates, page_number, path)
        elif page_number is not None:
            raise StoryValidationError(f"Unsupported numbered textless file: {relative_path(root, path)}")
        else:
            warnings.append(
                f"Ignored for upload but included in source fingerprint: {relative_path(root, path)}"
            )
    cover_path, cover_warnings = _select_cover(root, directory.name, cover_candidates)
    illustrations, illustration_warnings = _select_numbered_candidates(
        root, directory.name, "textless illustration", illustration_candidates
    )
    override_paths: set[Path] = set()
    for page_number, supplied_path in sorted(page_overrides.items()):
        if page_number < 1 or page_number > page_count:
            raise StoryValidationError(
                f"Textless page override {page_number} is outside 1..{page_count}"
            )
        candidate = (root / Path(supplied_path)).resolve()
        try:
            candidate.relative_to(directory.resolve())
        except ValueError as exception:
            raise StoryValidationError(
                f"Textless page override must stay inside {relative_path(root, directory)}: "
                f"{supplied_path}"
            ) from exception
        if not candidate.is_file():
            raise StoryValidationError(f"Textless page override file does not exist: {supplied_path}")
        if candidate.suffix.casefold() not in IMAGE_SUFFIXES:
            raise StoryValidationError(f"Textless page override is not a supported image: {supplied_path}")
        illustrations[page_number] = candidate
        override_paths.add(candidate)
        illustration_warnings.append(
            f"Explicit textless page override: page {page_number} uses {relative_path(root, candidate)}"
        )
    if len(set(illustrations.values())) != len(illustrations):
        raise StoryValidationError("A textless illustration file cannot be assigned to multiple pages")
    _validate_image_file(cover_path)
    for path in illustrations.values():
        _validate_image_file(path)
    override_relative_paths = {relative_path(root, path) for path in override_paths}
    warnings = [
        warning
        for warning in warnings
        if not any(warning.endswith(path) for path in override_relative_paths)
    ]
    warnings.extend(cover_warnings + illustration_warnings)
    _require_page_numbers(directory.name, "textless illustrations", illustrations, page_count)
    return TextlessInventory(cover_path, illustrations, tuple(warnings))


def _add_candidate(target: dict[int, list[Path]], number: int, path: Path) -> None:
    target.setdefault(number, []).append(path)


def _select_cover(root: Path, directory_name: str, candidates: list[Path]) -> tuple[Path, list[str]]:
    if not candidates:
        raise StoryValidationError(f"{directory_name} must contain a kapak/cover image; found 0")
    return _select_candidate(root, directory_name, "cover", candidates, _cover_priority)


def _select_numbered_candidates(
    root: Path,
    directory_name: str,
    label: str,
    candidates: dict[int, list[Path]],
) -> tuple[dict[int, Path], list[str]]:
    selected: dict[int, Path] = {}
    warnings: list[str] = []
    for page_number, paths in sorted(candidates.items()):
        selected_path, selection_warnings = _select_candidate(
            root,
            directory_name,
            f"page {page_number} {label}",
            paths,
            _page_candidate_priority,
        )
        selected[page_number] = selected_path
        warnings.extend(selection_warnings)
    return selected, warnings


def _select_candidate(
    root: Path,
    directory_name: str,
    label: str,
    candidates: list[Path],
    priority: Callable[[Path], int],
) -> tuple[Path, list[str]]:
    best_priority = min(priority(path) for path in candidates)
    best = [path for path in candidates if priority(path) == best_priority]
    if len(best) != 1:
        names = sorted(path.name for path in best)
        raise StoryValidationError(f"Ambiguous {directory_name} {label} candidates: {names}")
    selected = best[0]
    ignored = sorted((path for path in candidates if path != selected), key=lambda path: path.name.casefold())
    warnings = [
        f"Selected {relative_path(root, selected)} for {label}; ignored lower-priority candidate "
        f"{relative_path(root, path)}"
        for path in ignored
    ]
    return selected, warnings


def _leading_page_number(stem: str) -> int | None:
    match = re.match(r"^\s*(\d+)(?=$|\D)", stem)
    return int(match.group(1)) if match else None


def _is_cover_name(stem: str) -> bool:
    return bool({"kapak", "cover"} & set(_filename_tokens(stem)))


def _filename_tokens(stem: str) -> list[str]:
    return [token for token in re.split(r"[\W_]+", normalize_key(stem)) if token]


def _cover_priority(path: Path) -> int:
    tokens = _filename_tokens(path.stem)
    if tokens in (["kapak"], ["cover"]):
        return 0
    if {"revize", "revised"} & set(tokens):
        return 1
    return 2


def _page_candidate_priority(path: Path) -> int:
    if re.fullmatch(r"\s*\d+[\s._-]*", path.stem):
        return 0
    if {"revize", "revised"} & set(_filename_tokens(path.stem)):
        return 1
    return 2


def _validate_image_file(path: Path) -> None:
    header = _read_media_header(path, 8)
    suffix = path.suffix.casefold()
    if suffix in {".jpg", ".jpeg"} and not header.startswith(b"\xff\xd8\xff"):
        raise StoryValidationError(f"JPEG signature is invalid: {path}")
    if suffix == ".png" and header != b"\x89PNG\r\n\x1a\n":
        raise StoryValidationError(f"PNG signature is invalid: {path}")
    if suffix == ".gif" and header[:6] not in {b"GIF87a", b"GIF89a"}:
        raise StoryValidationError(f"GIF signature is invalid: {path}")


def _validate_mp3_file(path: Path) -> None:
    header = _read_media_header(path, 3)
    has_id3 = header.startswith(b"ID3")
    has_frame_sync = len(header) >= 2 and header[0] == 0xFF and header[1] & 0xE0 == 0xE0
    if not has_id3 and not has_frame_sync:
        raise StoryValidationError(f"MP3 signature is invalid: {path}")


def _read_media_header(path: Path, length: int) -> bytes:
    if path.stat().st_size == 0:
        raise StoryValidationError(f"Media file is empty: {path}")
    with path.open("rb") as source:
        return source.read(length)


def _require_page_numbers(directory_name: str, label: str, files: dict[int, Path], page_count: int) -> None:
    expected = set(range(1, page_count + 1))
    actual = set(files)
    if expected == actual:
        return
    missing = sorted(expected - actual)
    unexpected = sorted(actual - expected)
    raise StoryValidationError(
        f"{directory_name} {label} must cover 1..{page_count}; missing={missing}, unexpected={unexpected}"
    )


def _page_marker_body(paragraph: str, page_number: int) -> str | None:
    match = re.match(
        rf"^\s*{page_number}(?:(?:[.)-])\s*|\s+|$)(.*)$",
        paragraph,
        flags=re.DOTALL,
    )
    if not match:
        return None
    return normalize_text(match.group(1), keep_empty=True)


def _finalize_page_text(path: Path, page_number: int, lines: list[str]) -> str:
    body = normalize_text("\n".join(lines))
    if not body:
        raise StoryValidationError(f"DOCX {path.name} page {page_number} has empty body text")
    return body


def _build_contributor_assignments(
    localizations: Iterable[StoryLocalizationPlan],
) -> tuple[ContributorAssignmentPlan, ...]:
    localization_list = list(localizations)
    assignments: list[ContributorAssignmentPlan] = []
    for attribute, role in (("author", "AUTHOR"), ("illustrator", "ILLUSTRATOR"), ("narrator", "NARRATOR")):
        names = [getattr(localization, attribute) for localization in localization_list]
        if len({normalize_key(name) for name in names}) == 1:
            assignments.append(ContributorAssignmentPlan(names[0], role, None))
            continue
        assignments.extend(
            ContributorAssignmentPlan(getattr(localization, attribute), role, localization.language_code)
            for localization in localization_list
        )
    return tuple(assignments)


def _duplicates(values: Iterable[str]) -> list[str]:
    counts: dict[str, int] = {}
    for value in values:
        counts[value] = counts.get(value, 0) + 1
    return sorted(value for value, count in counts.items() if count > 1)
