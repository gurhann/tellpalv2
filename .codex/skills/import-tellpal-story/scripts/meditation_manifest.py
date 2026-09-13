from __future__ import annotations

import csv
import hashlib
import re
import tempfile
import unicodedata
import zipfile
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Mapping
import xml.etree.ElementTree as ElementTree

from lullaby_manifest import (
    StorageAssetStager,
    _extract_single_audio,
    _join_object_name,
    _validate_image,
    audio_content_fingerprint,
    audio_duration_minutes,
    sha256_file,
)
from story_manifest import StoryValidationError, normalize_text, slugify_turkish_title


SUPPORTED_LANGUAGES = {"tr", "en", "es", "pt", "de"}
REQUIRED_COLUMNS = {"language", "id", "name", "summary", "image_url", "summary_image_url"}
BODY_SUFFIXES = {".txt", ".md", ".docx"}
MAX_EXTERNAL_KEY_LENGTH = 180
MAX_LOCALIZATION_TITLE_LENGTH = 255


@dataclass(frozen=True)
class MeditationLocalizationPlan:
    language_code: str
    legacy_id: int
    line_number: int
    title: str
    description: str | None
    body_text: str | None
    body_source_path: str | None
    audio_media_key: str
    audio_source_name: str
    audio_path: str
    audio_content_fingerprint: str
    duration_minutes: int


@dataclass(frozen=True)
class MeditationMediaPlan:
    key: str
    source_name: str
    local_path: str
    kind: str
    checksum_sha256: str
    byte_size: int


@dataclass(frozen=True)
class MeditationSourceFile:
    path: str
    byte_size: int
    checksum_sha256: str


@dataclass(frozen=True)
class MeditationGroupPlan:
    external_key: str
    group_stem: str
    listening_cover_name: str
    listening_cover_media_key: str
    localizations: tuple[MeditationLocalizationPlan, ...]

    @property
    def cover_name(self) -> str:
        return self.listening_cover_name

    @property
    def cover_media_key(self) -> str:
        return self.listening_cover_media_key

    @property
    def languages(self) -> tuple[str, ...]:
        return tuple(item.language_code for item in self.localizations)

    @property
    def source_stem(self) -> str:
        return self.group_stem

    @property
    def legacy_ids(self) -> tuple[int, ...]:
        return tuple(item.legacy_id for item in self.localizations)

    @property
    def audio_source_ids(self) -> tuple[int, ...]:
        return self.legacy_ids


@dataclass(frozen=True)
class MeditationPlan:
    source_directory: str
    csv_path: str
    storage_base_url: str
    storage_bucket: str
    cover_prefix: str
    audio_prefix: str
    active: bool
    publish: bool
    groups: tuple[MeditationGroupPlan, ...]
    media: tuple[MeditationMediaPlan, ...]
    warnings: tuple[str, ...]
    missing_body_sources: tuple[str, ...]
    source_files: tuple[MeditationSourceFile, ...]
    source_fingerprint: str
    expected_actions: dict[str, int]
    allow_missing_body: bool = False

    def to_dict(self) -> dict[str, object]:
        return asdict(self)

    def media_by_key(self, key: str) -> MeditationMediaPlan:
        for media in self.media:
            if media.key == key:
                return media
        raise StoryValidationError(f"Missing media plan for {key}")

    @property
    def live_import_available(self) -> bool:
        return not self.missing_body_sources or (
            self.allow_missing_body and not self.publish
        )

    @property
    def staged_import(self) -> bool:
        """Whether this plan stages one or more bodyless draft localizations."""
        return bool(self.allow_missing_body and self.missing_body_sources)


def build_meditation_plan(
    csv_path: str | Path,
    *,
    storage_base_url: str = "https://storage.googleapis.com",
    storage_bucket: str = "tellpal-ee0dd.appspot.com",
    cover_prefix: str = "cover_images",
    audio_prefix: str = "",
    cache_directory: str | Path | None = None,
    active: bool = True,
    publish: bool = True,
    allow_missing_body: bool = False,
    external_key: str | None = None,
    service_account_json: str | Path | None = None,
    timeout_seconds: float = 120,
    body_sources: Mapping[object, object] | None = None,
    body_directory: str | Path | None = None,
    body_text_sources: Mapping[object, object] | None = None,
) -> MeditationPlan:
    if allow_missing_body and publish:
        raise StoryValidationError("--allow-missing-body requires --no-publish")

    csv_file = Path(csv_path).expanduser().resolve()
    if not csv_file.is_file():
        raise StoryValidationError(f"Meditation CSV does not exist: {csv_file}")
    rows = _read_rows(csv_file)
    if not rows:
        raise StoryValidationError("Meditation CSV contains no data rows")

    cache = (
        Path(cache_directory).expanduser().resolve()
        if cache_directory
        else Path(tempfile.mkdtemp(prefix="tellpal-meditation-import-"))
    )
    stager = StorageAssetStager(
        storage_base_url,
        storage_bucket,
        cache,
        timeout_seconds,
        service_account_json,
    )

    grouped: dict[str, list[dict[str, object]]] = {}
    cover_names_by_stem: dict[str, set[str]] = {}
    for row in rows:
        image_name = _object_name(row["image_url"], "image_url", row["line_number"])
        summary_name = normalize_text(row["summary_image_url"], keep_empty=True)
        stem = normalize_cover_stem(image_name)
        row = {**row, "image_name": image_name, "summary_name": summary_name, "group_stem": stem}
        grouped.setdefault(stem, []).append(row)
        cover_names_by_stem.setdefault(stem, set()).add(image_name)

    for stem, group_rows in grouped.items():
        names = cover_names_by_stem[stem]
        if len(names) > 1:
            lines = [str(row["line_number"]) for row in group_rows]
            raise StoryValidationError(
                f"Ambiguous normalized group stem {stem!r}: image_url values {sorted(names)!r} "
                f"on lines {', '.join(lines)} cannot select one listening cover"
            )

    warnings: list[str] = []
    media_by_identity: dict[tuple[str, str], MeditationMediaPlan] = {}
    groups: list[MeditationGroupPlan] = []
    external_keys: set[str] = set()
    selected_external_key = normalize_text(external_key) if external_key else None
    matched_external_key = False

    for stem, group_rows in grouped.items():
        languages = [str(row["language"]) for row in group_rows]
        if len(set(languages)) != len(languages):
            duplicates = sorted({language for language in languages if languages.count(language) > 1})
            duplicate_lines = [str(row["line_number"]) for row in group_rows if str(row["language"]) in duplicates]
            raise StoryValidationError(
                f"Duplicate language in meditation group {stem!r}: {duplicates}; "
                f"lines {', '.join(duplicate_lines)}"
            )
        if "tr" not in languages:
            lines = ", ".join(str(row["line_number"]) for row in group_rows)
            raise StoryValidationError(
                f"Meditation group {stem!r} requires a Turkish row; found lines {lines}"
            )

        generated_key = f"meditation.{slugify_turkish_title(stem)}"
        if len(generated_key) > MAX_EXTERNAL_KEY_LENGTH:
            raise StoryValidationError(
                f"Meditation group {stem!r} generated external key exceeds "
                f"{MAX_EXTERNAL_KEY_LENGTH} characters: {generated_key!r}"
            )
        if generated_key in external_keys:
            raise StoryValidationError(f"Duplicate generated external key {generated_key!r} for group {stem!r}")
        external_keys.add(generated_key)
        if selected_external_key and generated_key != selected_external_key:
            continue
        matched_external_key = matched_external_key or bool(selected_external_key)

        for row in group_rows:
            title = str(row["name"])
            if len(title) > MAX_LOCALIZATION_TITLE_LENGTH:
                raise StoryValidationError(
                    f"Group {stem!r}, line {row['line_number']}: localization title exceeds "
                    f"{MAX_LOCALIZATION_TITLE_LENGTH} characters"
                )

        cover_name = str(group_rows[0]["image_name"])
        try:
            cover_path = stager.download(
                _join_object_name(cover_prefix, cover_name), Path(cover_name).suffix.lower()
            )
            _validate_image(cover_path, cover_name)
        except StoryValidationError as exception:
            raise StoryValidationError(f"Group {stem!r} cover {cover_name!r}: {exception}") from exception
        cover_media = _media_plan(
            f"image:{sha256_file(cover_path)}", cover_name, cover_path, "ORIGINAL_IMAGE"
        )
        media_by_identity[("image", cover_media.checksum_sha256)] = cover_media

        for row in group_rows:
            summary_name = str(row["summary_name"])
            if not summary_name:
                warnings.append(
                    f"{generated_key}: line {row['line_number']} summary_image_url is empty; ignored"
                )
            elif summary_name != str(row["image_name"]):
                warnings.append(
                    f"{generated_key}: line {row['line_number']} summary_image_url "
                    f"{summary_name!r} differs from image_url {row['image_name']!r}; ignored"
                )

        localizations: list[MeditationLocalizationPlan] = []
        for row in sorted(group_rows, key=lambda item: str(item["language"])):
            language = str(row["language"])
            legacy_id = int(row["legacy_id"])
            try:
                zip_path = stager.download(
                    _join_object_name(audio_prefix, f"{legacy_id}.zip"), ".zip"
                )
                audio_path = _extract_single_audio(zip_path, cache / "audio", legacy_id)
                duration_minutes = audio_duration_minutes(audio_path)
                if duration_minutes <= 0:
                    raise StoryValidationError("audio duration is not readable or positive")
            except StoryValidationError as exception:
                raise StoryValidationError(
                    f"Group {stem!r}, line {row['line_number']}, object {legacy_id}.zip: {exception}"
                ) from exception
            fingerprint = audio_content_fingerprint(audio_path)
            audio_media = _media_plan(
                f"audio:{fingerprint}", f"{legacy_id}.mp3", audio_path, "ORIGINAL_AUDIO"
            )
            media_by_identity.setdefault(("audio", audio_media.key), audio_media)
            localizations.append(
                MeditationLocalizationPlan(
                    language_code=language,
                    legacy_id=legacy_id,
                    line_number=int(row["line_number"]),
                    title=str(row["name"]),
                    description=str(row["summary"]) if str(row["summary"]) else None,
                    body_text=None,
                    body_source_path=None,
                    audio_media_key=audio_media.key,
                    audio_source_name=f"{legacy_id}.zip",
                    audio_path=str(audio_path),
                    audio_content_fingerprint=fingerprint,
                    duration_minutes=duration_minutes,
                )
            )

        groups.append(
            MeditationGroupPlan(
                external_key=generated_key,
                group_stem=stem,
                listening_cover_name=cover_name,
                listening_cover_media_key=cover_media.key,
                localizations=tuple(localizations),
            )
        )

    if selected_external_key and not matched_external_key:
        raise StoryValidationError(f"No meditation group matches --external-key {selected_external_key!r}")

    if body_sources is not None and body_text_sources is not None:
        raise StoryValidationError("Use only one of body_sources and body_text_sources")
    resolved_body_sources = _resolve_body_sources(
        body_sources or body_text_sources or {}, body_directory, groups
    )
    missing_body_sources: list[str] = []
    resolved_groups: list[MeditationGroupPlan] = []
    body_files: set[Path] = set()
    for group in groups:
        localizations: list[MeditationLocalizationPlan] = []
        for localization in group.localizations:
            key = (group.group_stem, localization.language_code)
            source = resolved_body_sources.get(key)
            body_text: str | None = None
            body_path: str | None = None
            if source is None:
                missing_body_sources.append(f"{group.external_key}/{localization.language_code}")
            else:
                body_text, body_path = _load_body_source(source, group.external_key, localization.language_code)
                if body_path:
                    body_files.add(Path(body_path))
            localizations.append(
                MeditationLocalizationPlan(
                    **{
                        **asdict(localization),
                        "body_text": body_text,
                        "body_source_path": body_path,
                    }
                )
            )
        resolved_groups.append(
            MeditationGroupPlan(
                **{
                    **asdict(group),
                    "localizations": tuple(localizations),
                }
            )
        )

    if missing_body_sources:
        missing_body_summary = ", ".join(missing_body_sources)
        if allow_missing_body and not publish:
            warnings.append(
                "Body text is intentionally staged and must be supplied before publication for: "
                + missing_body_summary
            )
        else:
            warnings.append(
                "Live import unavailable until body text is supplied for: "
                + missing_body_summary
            )

    source_files = tuple(
        MeditationSourceFile(str(path), path.stat().st_size, sha256_file(path))
        for path in sorted({csv_file, *body_files}, key=lambda item: str(item).casefold())
    )
    source_fingerprint = _source_fingerprint(source_files)
    media = tuple(media_by_identity.values())
    localization_count = sum(len(group.localizations) for group in resolved_groups)
    return MeditationPlan(
        source_directory=str(csv_file.parent),
        csv_path=str(csv_file),
        storage_base_url=storage_base_url.rstrip("/"),
        storage_bucket=storage_bucket,
        cover_prefix=cover_prefix.strip("/"),
        audio_prefix=audio_prefix.strip("/"),
        active=active,
        publish=publish,
        groups=tuple(resolved_groups),
        media=media,
        warnings=tuple(sorted(set(warnings))),
        missing_body_sources=tuple(missing_body_sources),
        source_files=source_files,
        source_fingerprint=source_fingerprint,
        expected_actions={
            "contents": len(resolved_groups),
            "media_uploads": len(media),
            "listening_cover_uploads": len({group.listening_cover_media_key for group in resolved_groups}),
            "audio_uploads": len({item.audio_media_key for group in resolved_groups for item in group.localizations}),
            "localizations": localization_count,
            "cover_updates": len(resolved_groups),
            "publications": localization_count if publish else 0,
        },
        allow_missing_body=allow_missing_body,
    )


def normalize_cover_stem(image_name: str) -> str:
    """Return the stable group key after removing legacy cover suffixes."""
    raw_stem = Path(image_name).stem
    stripped = re.sub(r"(?:[_\s-]+(?:kapak|cover)|(?:kapak|cover))$", "", raw_stem, flags=re.IGNORECASE)
    prepared = unicodedata.normalize("NFKD", stripped).encode("ascii", "ignore").decode("ascii").casefold()
    normalized = re.sub(r"[^a-z0-9]+", "_", prepared).strip("_")
    if not normalized:
        raise StoryValidationError(
            f"image_url {image_name!r} has an empty or ambiguous group stem after removing cover suffixes"
        )
    return normalized


def source_fingerprint(csv_path: str | Path, body_paths: tuple[str | Path, ...] = ()) -> str:
    csv_file = Path(csv_path).expanduser().resolve()
    files = tuple(
        MeditationSourceFile(str(path), path.stat().st_size, sha256_file(path))
        for path in sorted({csv_file, *(Path(item).expanduser().resolve() for item in body_paths)}, key=lambda item: str(item).casefold())
    )
    return _source_fingerprint(files)


def assert_source_unchanged(plan: MeditationPlan) -> None:
    current: list[MeditationSourceFile] = []
    for item in plan.source_files:
        path = Path(item.path)
        if not path.is_file():
            raise StoryValidationError(f"Source file disappeared after preview: {path}")
        current.append(MeditationSourceFile(str(path), path.stat().st_size, sha256_file(path)))
    if _source_fingerprint(tuple(current)) != plan.source_fingerprint:
        raise StoryValidationError("Meditation CSV/body sources changed after preview; rebuild the plan before importing")


def _read_rows(path: Path) -> list[dict[str, object]]:
    try:
        with path.open("r", encoding="utf-8-sig", newline="") as source:
            reader = csv.DictReader(source, strict=True)
            headers = [normalize_text(item).casefold() for item in (reader.fieldnames or [])]
            if not headers or any(not header for header in headers):
                raise StoryValidationError("meditations.csv has a blank CSV column name")
            duplicate_headers = sorted({header for header in headers if headers.count(header) > 1})
            if duplicate_headers:
                raise StoryValidationError(f"meditations.csv has duplicate columns: {duplicate_headers}")
            missing = sorted(REQUIRED_COLUMNS - set(headers))
            if missing:
                raise StoryValidationError(f"meditations.csv is missing required columns: {missing}")
            header_by_normalized_name = {
                normalized: original
                for normalized, original in zip(headers, reader.fieldnames or [])
            }
            result: list[dict[str, object]] = []
            seen_ids: set[int] = set()
            for line_number, row in enumerate(reader, start=2):
                if None in row:
                    raise StoryValidationError(
                        f"Line {line_number}: row has extra CSV fields; check delimiters and quoting"
                    )

                def value_for(column: str, *, keep_empty: bool = False) -> str:
                    value = row.get(header_by_normalized_name[column])
                    if value is None:
                        raise StoryValidationError(
                            f"Line {line_number}: missing value for CSV column {column!r}"
                        )
                    return normalize_text(value, keep_empty=keep_empty)

                language = value_for("language").casefold()
                if language not in SUPPORTED_LANGUAGES:
                    raise StoryValidationError(f"Line {line_number}: unsupported language {language!r}")
                try:
                    legacy_id = int(value_for("id"))
                except ValueError as exception:
                    raise StoryValidationError(f"Line {line_number}: id must be an integer") from exception
                if legacy_id <= 0 or legacy_id in seen_ids:
                    raise StoryValidationError(f"Line {line_number}: id must be unique and positive")
                seen_ids.add(legacy_id)
                title = value_for("name")
                if not title:
                    raise StoryValidationError(f"Line {line_number}: name must not be blank")
                result.append(
                    {
                        "line_number": line_number,
                        "language": language,
                        "legacy_id": legacy_id,
                        "name": title,
                        "summary": value_for("summary", keep_empty=True),
                        "image_url": value_for("image_url", keep_empty=True),
                        "summary_image_url": value_for("summary_image_url", keep_empty=True),
                    }
                )
    except (UnicodeDecodeError, csv.Error) as exception:
        raise StoryValidationError(f"Cannot parse meditations.csv: {exception}") from exception
    return result


def _object_name(value: object, field: str, line_number: int) -> str:
    name = normalize_text(value)
    path = Path(name)
    if (
        not name
        or path.is_absolute()
        or "/" in name
        or "\\" in name
        or path.name != name
        or name in {".", ".."}
    ):
        raise StoryValidationError(f"Line {line_number}: {field} must be a single safe object filename: {value!r}")
    if field == "image_url" and path.suffix.casefold() not in {".jpg", ".jpeg", ".png", ".gif"}:
        raise StoryValidationError(f"Line {line_number}: {field} must reference a supported image: {value!r}")
    if field == "summary_image_url" and path.suffix.casefold() not in {".jpg", ".jpeg", ".png", ".gif"}:
        raise StoryValidationError(f"Line {line_number}: {field} must reference a supported image: {value!r}")
    return name


def _media_plan(key: str, source_name: str, local_path: Path, kind: str) -> MeditationMediaPlan:
    return MeditationMediaPlan(key, source_name, str(local_path), kind, sha256_file(local_path), local_path.stat().st_size)


def _source_fingerprint(files: tuple[MeditationSourceFile, ...]) -> str:
    serialized = "\n".join(
        f"{item.path}\0{item.byte_size}\0{item.checksum_sha256}" for item in files
    ).encode("utf-8")
    return hashlib.sha256(serialized).hexdigest()


def _resolve_body_sources(
    body_sources: Mapping[object, object],
    body_directory: str | Path | None,
    groups: tuple[MeditationGroupPlan, ...] | list[MeditationGroupPlan],
) -> dict[tuple[str, str], object]:
    group_aliases: dict[str, str] = {}
    for group in groups:
        for alias in (group.group_stem, group.external_key, group.external_key.removeprefix("meditation.")):
            normalized_alias = normalize_body_key(alias)
            previous_group = group_aliases.get(normalized_alias)
            if previous_group is not None and previous_group != group.group_stem:
                raise StoryValidationError(
                    f"Ambiguous body group alias {alias!r}: matches both "
                    f"{previous_group!r} and {group.group_stem!r}"
                )
            group_aliases[normalized_alias] = group.group_stem
    resolved: dict[tuple[str, str], object] = {}
    if body_directory is not None:
        root = Path(body_directory).expanduser().resolve()
        if not root.is_dir():
            raise StoryValidationError(f"Body directory does not exist: {root}")
        for path in sorted(root.rglob("*")):
            if not path.is_file() or path.suffix.casefold() not in BODY_SUFFIXES:
                continue
            key = _body_filename_key(root, path)
            if key is None:
                continue
            group_alias, language = key
            canonical_group = group_aliases.get(normalize_body_key(group_alias))
            if canonical_group is None:
                continue
            key = (canonical_group, language)
            if key in resolved:
                raise StoryValidationError(f"Multiple body sources for {canonical_group}/{language}: {resolved[key]} and {path}")
            resolved[key] = path

    for raw_key, value in body_sources.items():
        if isinstance(value, Mapping):
            group_alias = str(raw_key)
            for raw_language, nested_value in value.items():
                _set_body_source(resolved, group_aliases, group_alias, str(raw_language), nested_value)
            continue
        group_alias, language = _parse_body_key(raw_key)
        _set_body_source(resolved, group_aliases, group_alias, language, value)
    return resolved


def normalize_body_key(value: str) -> str:
    prepared = unicodedata.normalize("NFKD", normalize_text(value)).encode("ascii", "ignore").decode("ascii")
    return re.sub(r"[^a-z0-9]+", "", prepared.casefold())


def _set_body_source(
    target: dict[tuple[str, str], object],
    aliases: dict[str, str],
    group_alias: str,
    language: str,
    value: object,
) -> None:
    language = normalize_text(language).casefold()
    if language not in SUPPORTED_LANGUAGES:
        raise StoryValidationError(f"Body source uses unsupported language {language!r}")
    canonical_group = aliases.get(normalize_body_key(group_alias))
    if canonical_group is None:
        raise StoryValidationError(f"Body source references unknown meditation group {group_alias!r}")
    key = (canonical_group, language)
    if key in target and target[key] != value:
        raise StoryValidationError(f"Multiple body sources for {canonical_group}/{language}")
    target[key] = value


def _parse_body_key(raw_key: object) -> tuple[str, str]:
    if isinstance(raw_key, (tuple, list)) and len(raw_key) == 2:
        return str(raw_key[0]), str(raw_key[1])
    text = normalize_text(raw_key)
    match = re.match(r"^(.+?)(?:::|/|:|[._-])(tr|en|es|pt|de)$", text, flags=re.IGNORECASE)
    if not match:
        raise StoryValidationError(
            f"Body source key {raw_key!r} must identify a group and language, e.g. best_friend/tr"
        )
    return match.group(1), match.group(2)


def _body_filename_key(root: Path, path: Path) -> tuple[str, str] | None:
    relative = path.relative_to(root)
    if len(relative.parts) >= 2 and relative.parent != Path("."):
        language = relative.stem.casefold()
        if language not in SUPPORTED_LANGUAGES:
            return None
        return relative.parent.as_posix(), language
    match = re.match(r"^(.+?)(?:[._-])(tr|en|es|pt|de)$", path.stem, flags=re.IGNORECASE)
    if not match:
        return None
    return match.group(1), match.group(2).casefold()


def _load_body_source(source: object, external_key: str, language: str) -> tuple[str, str | None]:
    if isinstance(source, Path):
        path = source.expanduser().resolve()
    elif isinstance(source, str):
        try:
            candidate = Path(source).expanduser()
            path = candidate.resolve() if candidate.is_file() else None
        except OSError:
            path = None
        if path is None:
            if _looks_like_body_path(source):
                raise StoryValidationError(f"Body source for {external_key}/{language} does not exist: {source}")
            body = normalize_text(source, keep_empty=True)
            if not body:
                raise StoryValidationError(f"Body text is empty for {external_key}/{language}")
            return body, None
    else:
        try:
            path = Path(source).expanduser().resolve()  # type: ignore[arg-type]
        except (TypeError, OSError) as exception:
            raise StoryValidationError(f"Body source for {external_key}/{language} is not a file or text") from exception
    if not path.is_file():
        raise StoryValidationError(f"Body source for {external_key}/{language} does not exist: {path}")
    if path.suffix.casefold() not in BODY_SUFFIXES:
        raise StoryValidationError(f"Body source for {external_key}/{language} has unsupported type: {path.suffix}")
    try:
        if path.suffix.casefold() == ".docx":
            body = _read_docx_body(path)
        else:
            body = normalize_text(path.read_text(encoding="utf-8-sig"), keep_empty=True)
    except (OSError, UnicodeDecodeError, zipfile.BadZipFile, KeyError, ElementTree.ParseError) as exception:
        raise StoryValidationError(f"Cannot read body source {path}: {exception}") from exception
    if not body:
        raise StoryValidationError(f"Body text is empty for {external_key}/{language}: {path}")
    return body, str(path)


def _looks_like_body_path(value: str) -> bool:
    return (
        "\\" in value
        or "/" in value
        or Path(value).suffix.casefold() in BODY_SUFFIXES
        or value.startswith(".")
    )


def _read_docx_body(path: Path) -> str:
    with zipfile.ZipFile(path) as archive:
        xml = archive.read("word/document.xml")
    root = ElementTree.fromstring(xml)
    paragraphs: list[str] = []
    for paragraph in root.iter("{http://schemas.openxmlformats.org/wordprocessingml/2006/main}p"):
        chunks: list[str] = []
        for node in paragraph.iter():
            local_name = node.tag.rsplit("}", 1)[-1]
            if local_name == "t":
                chunks.append(node.text or "")
            elif local_name in {"br", "cr"}:
                chunks.append("\n")
            elif local_name == "tab":
                chunks.append("\t")
        paragraphs.append("".join(chunks))
    return normalize_text("\n".join(paragraphs), keep_empty=True)


__all__ = [
    "MeditationGroupPlan",
    "MeditationLocalizationPlan",
    "MeditationMediaPlan",
    "MeditationPlan",
    "MeditationSourceFile",
    "StorageAssetStager",
    "StoryValidationError",
    "_extract_single_audio",
    "assert_source_unchanged",
    "audio_content_fingerprint",
    "audio_duration_minutes",
    "build_meditation_plan",
    "normalize_cover_stem",
    "source_fingerprint",
]
