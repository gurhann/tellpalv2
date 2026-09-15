from __future__ import annotations

import csv
import math
import re
import tempfile
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Iterable

from lullaby_manifest import (
    StorageAssetStager,
    StorageAssetError,
    _extract_single_audio,
    _join_object_name,
    _object_name,
    _validate_image,
    audio_duration_minutes,
    sha256_file,
)
from story_manifest import StoryValidationError, normalize_text


SUPPORTED_LANGUAGES = {"tr", "en", "es", "pt", "de"}
REQUIRED_COLUMNS = {"language", "id", "name", "image_url"}
STATUS_COLUMNS = (
    "status",
    "error",
    "content_id",
    "audio_asset_id",
    "cover_asset_id",
    "duration_minutes",
    "processed_at",
)


@dataclass
class AudioStoryRowPlan:
    line_number: int
    language_code: str
    legacy_id_text: str
    legacy_id: int | None
    title: str
    image_name: str
    duplicate_of_line: int | None = None
    status: str = "PENDING"
    error: str = ""
    audio_media_key: str | None = None
    cover_media_key: str | None = None
    audio_path: str | None = None
    cover_path: str | None = None
    audio_checksum_sha256: str | None = None
    cover_checksum_sha256: str | None = None
    duration_minutes: int | None = None
    content_id: int | None = None
    audio_asset_id: int | None = None
    cover_asset_id: int | None = None
    processed_at: str = ""

    @property
    def match_key(self) -> str:
        return _match_key(self.title)

    @property
    def duplicate_key(self) -> tuple[str, str, str, str]:
        return (
            self.language_code,
            self.legacy_id_text,
            self.title,
            self.image_name,
        )

    def to_status_row(self) -> dict[str, object]:
        return {
            "language": self.language_code,
            "id": self.legacy_id_text,
            "name": self.title,
            "image_url": self.image_name,
            "status": self.status,
            "error": self.error,
            "content_id": self.content_id or "",
            "audio_asset_id": self.audio_asset_id or "",
            "cover_asset_id": self.cover_asset_id or "",
            "duration_minutes": self.duration_minutes or "",
            "processed_at": self.processed_at,
        }


@dataclass(frozen=True)
class AudioStoryMediaPlan:
    key: str
    source_name: str
    local_path: str
    kind: str
    checksum_sha256: str
    byte_size: int


@dataclass
class AudioStoryPlan:
    csv_path: str
    source_directory: str
    storage_base_url: str
    storage_bucket: str
    cover_prefix: str
    audio_prefix: str
    status_csv_path: str
    rows: tuple[AudioStoryRowPlan, ...]
    media: tuple[AudioStoryMediaPlan, ...]
    warnings: tuple[str, ...]
    source_fingerprint: str
    expected_actions: dict[str, int]

    def to_dict(self) -> dict[str, object]:
        return asdict(self)

    def media_by_key(self, key: str) -> AudioStoryMediaPlan:
        for media in self.media:
            if media.key == key:
                return media
        raise StoryValidationError(f"Missing media plan for {key}")


def build_audio_story_plan(
    csv_path: str | Path,
    *,
    storage_base_url: str = "https://storage.googleapis.com",
    storage_bucket: str = "tellpal-ee0dd.appspot.com",
    cover_prefix: str = "cover_images",
    audio_prefix: str = "",
    cache_directory: str | Path | None = None,
    status_csv_path: str | Path | None = None,
    service_account_json: str | Path | None = None,
    timeout_seconds: float = 120,
) -> AudioStoryPlan:
    csv_file = Path(csv_path).expanduser().resolve()
    if not csv_file.is_file():
        raise StoryValidationError(f"Audio-story CSV does not exist: {csv_file}")
    if not math.isfinite(timeout_seconds) or timeout_seconds <= 0:
        raise StoryValidationError("timeout_seconds must be a finite positive number")
    rows = _read_rows(csv_file)
    cache = Path(cache_directory) if cache_directory else Path(tempfile.mkdtemp(prefix="tellpal-audio-story-"))
    stager = StorageAssetStager(
        storage_base_url,
        storage_bucket,
        cache,
        timeout_seconds,
        service_account_json,
    )
    warnings: list[str] = []
    plans: list[AudioStoryRowPlan] = []
    media_by_key: dict[str, AudioStoryMediaPlan] = {}
    seen: dict[tuple[str, str, str, str], int] = {}

    for raw in rows:
        row = _row_from_csv(raw)
        plans.append(row)
        if row.status == "ERROR":
            continue
        duplicate_line = seen.get(row.duplicate_key)
        if duplicate_line is not None:
            row.status = "SKIPPED_DUPLICATE"
            row.duplicate_of_line = duplicate_line
            row.error = f"Identical row already represented by CSV line {duplicate_line}"
            continue
        seen[row.duplicate_key] = row.line_number
        try:
            image_name = _object_name(row.image_name, "image_url")
            row.image_name = image_name
            cover_path = stager.download(_join_object_name(cover_prefix, image_name), Path(image_name).suffix.lower())
            _validate_image(cover_path, image_name)
            zip_name = f"{row.legacy_id_text}.zip"
            zip_path = stager.download(_join_object_name(audio_prefix, zip_name), ".zip")
            audio_path = _extract_single_audio(zip_path, cache / "audio", row.legacy_id_text)
            duration = audio_duration_minutes(audio_path)
            if duration <= 0:
                raise StoryValidationError(f"{zip_name} does not have a positive readable duration")

            cover_checksum = sha256_file(cover_path)
            audio_checksum = sha256_file(audio_path)
            row.cover_path = str(cover_path)
            row.audio_path = str(audio_path)
            row.cover_checksum_sha256 = cover_checksum
            row.audio_checksum_sha256 = audio_checksum
            row.cover_media_key = f"image:{cover_checksum}"
            row.audio_media_key = f"audio:{audio_checksum}"
            row.duration_minutes = duration
            media_by_key.setdefault(
                row.cover_media_key,
                AudioStoryMediaPlan(
                    row.cover_media_key,
                    image_name,
                    str(cover_path),
                    "ORIGINAL_IMAGE",
                    cover_checksum,
                    cover_path.stat().st_size,
                ),
            )
            media_by_key.setdefault(
                row.audio_media_key,
                AudioStoryMediaPlan(
                    row.audio_media_key,
                    f"{row.legacy_id}.mp3",
                    str(audio_path),
                    "ORIGINAL_AUDIO",
                    audio_checksum,
                    audio_path.stat().st_size,
                ),
            )
        except StorageAssetError as exception:
            if exception.fatal:
                raise
            row.status = "ERROR"
            row.error = str(exception)
        except (StoryValidationError, OSError, RuntimeError, ValueError) as exception:
            row.status = "ERROR"
            row.error = str(exception)

    valid_rows = sum(row.status == "PENDING" for row in plans)
    warnings.extend(
        f"line {row.line_number}: duplicate of line {row.duplicate_of_line}"
        for row in plans
        if row.status == "SKIPPED_DUPLICATE"
    )
    output_path = Path(status_csv_path).expanduser().resolve() if status_csv_path else csv_file.with_name(f"{csv_file.stem}.import.csv")
    if output_path == csv_file:
        raise StoryValidationError("Status CSV must be different from the source CSV")
    return AudioStoryPlan(
        csv_path=str(csv_file),
        source_directory=str(csv_file.parent),
        storage_base_url=storage_base_url.rstrip("/"),
        storage_bucket=storage_bucket,
        cover_prefix=cover_prefix.strip("/"),
        audio_prefix=audio_prefix.strip("/"),
        status_csv_path=str(output_path),
        rows=tuple(plans),
        media=tuple(media_by_key.values()),
        warnings=tuple(warnings),
        source_fingerprint=sha256_file(csv_file),
        expected_actions={
            "rows": len(plans),
            "validRows": valid_rows,
            "mediaUploads": len(media_by_key),
            "localErrors": sum(row.status == "ERROR" for row in plans),
            "duplicates": sum(row.status == "SKIPPED_DUPLICATE" for row in plans),
        },
    )


def assert_source_unchanged(plan: AudioStoryPlan) -> None:
    if sha256_file(plan.csv_path) != plan.source_fingerprint:
        raise StoryValidationError("audio_stories.csv changed after preview; rebuild the plan before importing")


def write_status_csv(plan: AudioStoryPlan) -> None:
    output = Path(plan.status_csv_path)
    output.parent.mkdir(parents=True, exist_ok=True)
    temporary = output.with_suffix(f"{output.suffix}.tmp")
    fieldnames = ["language", "id", "name", "image_url", *STATUS_COLUMNS]
    with temporary.open("w", encoding="utf-8-sig", newline="") as target:
        writer = csv.DictWriter(target, fieldnames=fieldnames, extrasaction="ignore")
        writer.writeheader()
        writer.writerows(row.to_status_row() for row in plan.rows)
    temporary.replace(output)


def _read_rows(path: Path) -> list[dict[str, object]]:
    try:
        with path.open("r", encoding="utf-8-sig", newline="") as source:
            reader = csv.DictReader(source, strict=True)
            headers = [normalize_text(item).casefold() for item in (reader.fieldnames or [])]
            duplicate_headers = sorted({item for item in headers if headers.count(item) > 1})
            if duplicate_headers:
                raise StoryValidationError(f"audio_stories.csv has duplicate columns: {duplicate_headers}")
            missing = sorted(REQUIRED_COLUMNS - set(headers))
            if missing:
                raise StoryValidationError(f"audio_stories.csv is missing required columns: {missing}")
            canonical_headers = dict(zip(reader.fieldnames or [], headers))
            result: list[dict[str, object]] = []
            for line_number, row in enumerate(reader, start=2):
                canonical_row = {
                    canonical_headers[key]: value
                    for key, value in row.items()
                    if key is not None and canonical_headers.get(key)
                }
                if None in row:
                    result.append({"line_number": line_number, "_error": "CSV row contains extra fields", **canonical_row})
                else:
                    result.append({"line_number": line_number, **canonical_row})
            if not result:
                raise StoryValidationError("audio_stories.csv contains no data rows")
            return result
    except (UnicodeDecodeError, csv.Error) as exception:
        raise StoryValidationError(f"Cannot parse audio_stories.csv: {exception}") from exception


def _row_from_csv(raw: dict[str, object]) -> AudioStoryRowPlan:
    line_number = int(raw.get("line_number", 0))
    language = normalize_text(raw.get("language"))
    legacy_id_text = normalize_text(raw.get("id"))
    title = normalize_text(raw.get("name"))
    image_name = normalize_text(raw.get("image_url"))
    row = AudioStoryRowPlan(line_number, language, legacy_id_text, None, title, image_name)
    if raw.get("_error"):
        row.status = "ERROR"
        row.error = str(raw["_error"])
        return row
    if language not in SUPPORTED_LANGUAGES:
        row.status = "ERROR"
        row.error = f"Unsupported language {language!r}"
        return row
    if not legacy_id_text:
        row.status = "ERROR"
        row.error = "id must not be blank"
        return row
    try:
        row.legacy_id = int(legacy_id_text)
    except ValueError:
        row.status = "ERROR"
        row.error = "id must be an integer"
        return row
    if row.legacy_id <= 0:
        row.status = "ERROR"
        row.error = "id must be positive"
        return row
    if not title:
        row.status = "ERROR"
        row.error = "name must not be blank"
        return row
    if not image_name:
        row.status = "ERROR"
        row.error = "image_url must not be blank"
    return row


def _match_key(value: object) -> str:
    return re.sub(r"\s+", " ", normalize_text(value)).casefold()
