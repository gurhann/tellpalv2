from __future__ import annotations

import csv
import hashlib
import math
import re
import tempfile
import unicodedata
import urllib.parse
import urllib.request
import zipfile
from dataclasses import asdict, dataclass
from pathlib import Path, PurePosixPath

from story_manifest import StoryValidationError, normalize_text, slugify_turkish_title


SUPPORTED_LANGUAGES = {"tr", "en", "es", "pt", "de"}
REQUIRED_COLUMNS = {"language", "id", "name", "summary", "image_url", "summary_image_url"}
IMAGE_SUFFIXES = {".jpg", ".jpeg", ".png", ".gif"}
MAX_EXTRACTED_AUDIO_BYTES = 64 * 1024 * 1024


@dataclass(frozen=True)
class LullabyLocalizationPlan:
    language_code: str
    legacy_id: int
    title: str
    musician: str | None


@dataclass(frozen=True)
class LullabyMediaPlan:
    key: str
    source_name: str
    local_path: str
    kind: str
    checksum_sha256: str
    byte_size: int


@dataclass(frozen=True)
class LullabyGroupPlan:
    external_key: str
    listing_cover_name: str
    listening_cover_name: str
    listing_media_key: str
    listening_media_key: str
    audio_media_key: str
    audio_source_ids: tuple[int, ...]
    audio_path: str
    audio_content_fingerprint: str
    duration_minutes: int
    musician: str | None
    instrument_codes: tuple[str, ...]
    unsupported_instruments: tuple[str, ...]
    localizations: tuple[LullabyLocalizationPlan, ...]


@dataclass(frozen=True)
class LullabyPlan:
    source_directory: str
    csv_path: str
    storage_base_url: str
    storage_bucket: str
    cover_prefix: str
    audio_prefix: str
    active: bool
    publish: bool
    groups: tuple[LullabyGroupPlan, ...]
    media: tuple[LullabyMediaPlan, ...]
    warnings: tuple[str, ...]
    source_fingerprint: str
    expected_actions: dict[str, int]

    def to_dict(self) -> dict[str, object]:
        return asdict(self)

    def media_by_key(self, key: str) -> LullabyMediaPlan:
        for media in self.media:
            if media.key == key:
                return media
        raise StoryValidationError(f"Missing media plan for {key}")


class StorageAssetStager:
    """Downloads public bucket objects into a run-local cache without touching the source folder."""

    def __init__(self, base_url: str, bucket: str, cache_directory: Path, timeout_seconds: float):
        self.base_url = base_url.rstrip("/")
        self.bucket = bucket.strip()
        self.cache_directory = cache_directory
        self.timeout_seconds = timeout_seconds
        self.cache_directory.mkdir(parents=True, exist_ok=True)

    def object_url(self, object_name: str) -> str:
        encoded_bucket = urllib.parse.quote(self.bucket, safe="")
        encoded_object = urllib.parse.quote(object_name, safe="/")
        return f"{self.base_url}/{encoded_bucket}/{encoded_object}"

    def download(self, object_name: str, suffix: str) -> Path:
        safe_name = hashlib.sha256(object_name.encode("utf-8")).hexdigest() + suffix
        destination = self.cache_directory / safe_name
        url = self.object_url(object_name)
        try:
            with urllib.request.urlopen(url, timeout=self.timeout_seconds) as response, destination.open("wb") as target:
                content_length = response.headers.get("Content-Length")
                if content_length and int(content_length) > 250 * 1024 * 1024:
                    raise StoryValidationError(f"Remote object is unexpectedly large: {object_name}")
                total = 0
                while True:
                    block = response.read(1024 * 1024)
                    if not block:
                        break
                    total += len(block)
                    if total > 250 * 1024 * 1024:
                        raise StoryValidationError(f"Remote object exceeds 250 MiB limit: {object_name}")
                    target.write(block)
        except Exception as exception:
            destination.unlink(missing_ok=True)
            raise StoryValidationError(f"Cannot download storage object {object_name!r} from {url}: {exception}") from exception
        if destination.stat().st_size == 0:
            raise StoryValidationError(f"Storage object is empty: {object_name}")
        return destination


def build_lullaby_plan(
    csv_path: str | Path,
    *,
    storage_base_url: str = "https://storage.googleapis.com",
    storage_bucket: str = "tellpal-ee0dd.appspot.com",
    cover_prefix: str = "cover_images",
    audio_prefix: str = "",
    cache_directory: str | Path | None = None,
    active: bool = True,
    publish: bool = True,
    duration_override: int | None = None,
    timeout_seconds: float = 120,
) -> LullabyPlan:
    csv_file = Path(csv_path).expanduser().resolve()
    if not csv_file.is_file():
        raise StoryValidationError(f"Lullaby CSV does not exist: {csv_file}")
    rows = _read_rows(csv_file)
    if not rows:
        raise StoryValidationError("Lullaby CSV contains no data rows")
    if duration_override is not None and duration_override <= 0:
        raise StoryValidationError("--duration-minutes must be a positive integer")

    root = csv_file.parent
    cache = Path(cache_directory).expanduser().resolve() if cache_directory else Path(
        tempfile.mkdtemp(prefix="tellpal-lullaby-import-")
    )
    stager = StorageAssetStager(storage_base_url, storage_bucket, cache, timeout_seconds)
    grouped: dict[tuple[str, str], list[dict[str, object]]] = {}
    casefolded_pairs: dict[tuple[str, str], tuple[str, str]] = {}
    for row in rows:
        listing = _object_name(row["image_url"], "image_url")
        listening = _object_name(row["summary_image_url"], "summary_image_url")
        cover_pair = (listing, listening)
        folded_pair = (listing.casefold(), listening.casefold())
        previous_pair = casefolded_pairs.setdefault(folded_pair, cover_pair)
        if previous_pair != cover_pair:
            raise StoryValidationError(
                "Cover object names differ only by case and cannot be grouped safely: "
                f"{previous_pair} versus {cover_pair}"
            )
        grouped.setdefault(cover_pair, []).append(
            {**row, "listing": listing, "listening": listening}
        )

    warnings: list[str] = []
    media_by_identity: dict[tuple[str, str], LullabyMediaPlan] = {}
    groups: list[LullabyGroupPlan] = []
    external_keys: set[str] = set()

    for group_rows in grouped.values():
        languages = [str(row["language"]) for row in group_rows]
        if len(set(languages)) != len(languages):
            raise StoryValidationError(f"Duplicate language in cover group: {languages}")
        if "tr" not in languages:
            raise StoryValidationError("Every lullaby cover group requires a Turkish row")
        turkish = next(row for row in group_rows if row["language"] == "tr")
        external_key = f"lullaby.{slugify_turkish_title(str(turkish['name']))}"
        if external_key in external_keys:
            raise StoryValidationError(f"Duplicate generated external key: {external_key}")
        external_keys.add(external_key)

        listing_name = str(group_rows[0]["listing"])
        listening_name = str(group_rows[0]["listening"])
        listing_path = stager.download(_join_object_name(cover_prefix, listing_name), Path(listing_name).suffix.lower())
        listening_path = listing_path if listing_name == listening_name else stager.download(
            _join_object_name(cover_prefix, listening_name), Path(listening_name).suffix.lower()
        )
        _validate_image(listing_path, listing_name)
        _validate_image(listening_path, listening_name)
        listing_media = _media_plan(
            f"image:{sha256_file(listing_path)}", listing_name, listing_path, "ORIGINAL_IMAGE"
        )
        listening_media = listing_media if listing_name == listening_name else _media_plan(
            f"image:{sha256_file(listening_path)}", listening_name, listening_path, "ORIGINAL_IMAGE"
        )
        media_by_identity[("image", listing_media.checksum_sha256)] = listing_media
        media_by_identity[("image", listening_media.checksum_sha256)] = listening_media

        audio_paths: list[Path] = []
        audio_fingerprints: list[str] = []
        audio_ids: list[int] = []
        for row in group_rows:
            legacy_id = int(row["legacy_id"])
            audio_ids.append(legacy_id)
            zip_path = stager.download(_join_object_name(audio_prefix, f"{legacy_id}.zip"), ".zip")
            audio_path = _extract_single_audio(zip_path, cache / "audio", legacy_id)
            fingerprint = audio_content_fingerprint(audio_path)
            audio_paths.append(audio_path)
            audio_fingerprints.append(fingerprint)
        if len(set(audio_fingerprints)) != 1:
            raise StoryValidationError(
                f"Shared lullaby playback cannot choose different audio files for {external_key}; "
                f"legacy ids={audio_ids}"
            )
        audio_path = audio_paths[0]
        duration_minutes = audio_duration_minutes(audio_path)
        if duration_minutes <= 0:
            duration_minutes = duration_override or 0
        if duration_minutes <= 0:
            raise StoryValidationError(
                f"Could not determine a positive duration for {external_key}; use --duration-minutes"
            )
        audio_media = _media_plan(
            f"audio:{audio_fingerprints[0]}", f"{audio_ids[0]}.mp3", audio_path, "ORIGINAL_AUDIO"
        )
        media_by_identity[("audio", audio_media.checksum_sha256)] = audio_media

        localizations: list[LullabyLocalizationPlan] = []
        musicians: set[str] = set()
        instrument_code_sets: list[tuple[str, ...]] = []
        for row in sorted(group_rows, key=lambda item: str(item["language"])):
            musician, instruments, unsupported = parse_summary(str(row["summary"]))
            if musician:
                musicians.add(musician)
            instrument_code_sets.append(instruments)
            for item in unsupported:
                warnings.append(f"{external_key}: instrument {item!r} is not in the current catalog and will be skipped")
            localizations.append(
                LullabyLocalizationPlan(
                    language_code=str(row["language"]),
                    legacy_id=int(row["legacy_id"]),
                    title=normalize_text(row["name"]),
                    musician=musician,
                )
            )
        if len(musicians) > 1:
            raise StoryValidationError(f"Lullaby has different MUSICIAN values across languages: {sorted(musicians)}")
        instrument_codes = tuple(dict.fromkeys(code for codes in instrument_code_sets for code in codes))
        if any(codes != instrument_code_sets[0] for codes in instrument_code_sets[1:]):
            warnings.append(f"{external_key}: instrument labels differ by language; merged in first-seen order")
        warnings.append(f"{external_key}: CSV summary is not stored on LULLABY localizations; title/publication state are the supported fields")
        groups.append(
            LullabyGroupPlan(
                external_key=external_key,
                listing_cover_name=listing_name,
                listening_cover_name=listening_name,
                listing_media_key=listing_media.key,
                listening_media_key=listening_media.key,
                audio_media_key=audio_media.key,
                audio_source_ids=tuple(audio_ids),
                audio_path=str(audio_path),
                audio_content_fingerprint=audio_fingerprints[0],
                duration_minutes=duration_minutes,
                musician=next(iter(musicians), None),
                instrument_codes=instrument_codes,
                unsupported_instruments=tuple(sorted({item for row in group_rows for item in parse_summary(str(row["summary"]))[2]})),
                localizations=tuple(localizations),
            )
        )

    media = tuple(media_by_identity.values())
    source_fingerprint = sha256_file(csv_file)
    return LullabyPlan(
        source_directory=str(root),
        csv_path=str(csv_file),
        storage_base_url=storage_base_url.rstrip("/"),
        storage_bucket=storage_bucket,
        cover_prefix=cover_prefix.strip("/"),
        audio_prefix=audio_prefix.strip("/"),
        active=active,
        publish=publish,
        groups=tuple(groups),
        media=media,
        warnings=tuple(sorted(set(warnings))),
        source_fingerprint=source_fingerprint,
        expected_actions={
            "contents": len(groups),
            "media_uploads": len(media),
            "localizations": sum(len(group.localizations) for group in groups),
            "playback_updates": len(groups),
            "cover_updates": len(groups),
            "instrument_updates": sum(bool(group.instrument_codes) for group in groups),
            "contributor_assignments": sum(bool(group.musician) for group in groups),
            "publications": sum(len(group.localizations) for group in groups) if publish else 0,
        },
    )


def source_fingerprint(csv_path: str | Path) -> str:
    return sha256_file(Path(csv_path).expanduser().resolve())


def assert_source_unchanged(plan: LullabyPlan) -> None:
    if source_fingerprint(plan.csv_path) != plan.source_fingerprint:
        raise StoryValidationError("lullabies.csv changed after preview; rebuild the plan before importing")


def parse_summary(summary: str) -> tuple[str | None, tuple[str, ...], tuple[str, ...]]:
    lines = [normalize_text(line) for line in summary.replace("\r", "").split("\n") if normalize_text(line)]
    musician_values: list[str] = []
    instrument_values: list[str] = []
    for line in lines:
        match = re.match(r"(?:music|müzik)\s*:\s*(.+)$", line, flags=re.IGNORECASE)
        if match:
            musician_values.append(normalize_text(match.group(1)))
        match = re.match(r"(?:instrument|enstrüman)\s*:\s*(.+)$", line, flags=re.IGNORECASE)
        if match:
            instrument_values.append(normalize_text(match.group(1)))
    if len(musician_values) > 1:
        raise StoryValidationError("Summary has ambiguous Music/Müzik values")
    if len(instrument_values) > 1:
        raise StoryValidationError("Summary has ambiguous Instrument/Enstrüman values")
    musician = musician_values[0] if musician_values else None
    instrument_text = instrument_values[0] if instrument_values else ""
    aliases = {
        "celesta": "CELESTA", "chelesta": "CELESTA", "celeste": "CELESTA", "celesta": "CELESTA",
        "bell": "BELL", "keman": "VIOLIN", "violin": "VIOLIN", "rhodes": "RHODES",
        "glockenspiel": "GLOCKENSPIEL", "arp": "HARP", "harp": "HARP", "vibrafon": "VIBRAPHONE",
        "vibraphone": "VIBRAPHONE", "yayli orkestra": "STRING_ORCHESTRA", "string orchestra": "STRING_ORCHESTRA",
    }
    unsupported_aliases = {"piano", "piyano"}
    codes: list[str] = []
    unsupported: list[str] = []
    tokens = [normalize_text(token) for token in re.split(r"\s*(?:,|;|/|&|\+|\band\b|\bve\b|-)\s*", instrument_text, flags=re.IGNORECASE) if normalize_text(token)]
    for token in tokens:
        key = _ascii_key(token)
        code = aliases.get(key)
        if code and code not in codes:
            codes.append(code)
        elif key in unsupported_aliases:
            unsupported.append(token)
        elif token:
            unsupported.append(token)
    return musician, tuple(codes), tuple(unsupported)


def audio_duration_minutes(path: str | Path) -> int:
    seconds = _mp3_duration_seconds(Path(path))
    return max(1, math.ceil(seconds / 60)) if seconds > 0 else 0


def audio_content_fingerprint(path: str | Path) -> str:
    data = Path(path).read_bytes()
    start = _skip_id3v2(data)
    end = len(data)
    if end >= 128 and data[-128:-125] == b"TAG":
        end -= 128
    return hashlib.sha256(data[start:end]).hexdigest()


def sha256_file(path: str | Path) -> str:
    digest = hashlib.sha256()
    with Path(path).open("rb") as source:
        for block in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def _read_rows(path: Path) -> list[dict[str, object]]:
    with path.open("r", encoding="utf-8-sig", newline="") as source:
        reader = csv.DictReader(source)
        columns = {normalize_text(item) for item in (reader.fieldnames or [])}
        missing = sorted(REQUIRED_COLUMNS - columns)
        if missing:
            raise StoryValidationError(f"lullabies.csv is missing required columns: {missing}")
        result: list[dict[str, object]] = []
        seen_ids: set[int] = set()
        for line_number, row in enumerate(reader, start=2):
            language = normalize_text(row.get("language"))
            if language not in SUPPORTED_LANGUAGES:
                raise StoryValidationError(f"Line {line_number}: unsupported language {language!r}")
            try:
                legacy_id = int(normalize_text(row.get("id")))
            except ValueError as exception:
                raise StoryValidationError(f"Line {line_number}: id must be an integer") from exception
            if legacy_id <= 0 or legacy_id in seen_ids:
                raise StoryValidationError(f"Line {line_number}: id must be unique and positive")
            seen_ids.add(legacy_id)
            title = normalize_text(row.get("name"))
            if not title:
                raise StoryValidationError(f"Line {line_number}: name must not be blank")
            summary = normalize_text(row.get("summary"), keep_empty=True)
            result.append({"language": language, "legacy_id": legacy_id, "name": title, "summary": summary,
                           "image_url": normalize_text(row.get("image_url")), "summary_image_url": normalize_text(row.get("summary_image_url"))})
    return result


def _object_name(value: object, field: str) -> str:
    name = normalize_text(value)
    path = PurePosixPath(name)
    if not name or path.is_absolute() or "/" in name or "\\" in name or path.name != name or name in {".", ".."}:
        raise StoryValidationError(f"{field} must be a single object filename: {value!r}")
    if Path(name).suffix.lower() not in IMAGE_SUFFIXES:
        raise StoryValidationError(f"{field} must reference a supported image: {value!r}")
    return name


def _join_object_name(prefix: str, name: str) -> str:
    return "/".join(part.strip("/") for part in (prefix, name) if part.strip("/"))


def _media_plan(key: str, source_name: str, local_path: Path, kind: str) -> LullabyMediaPlan:
    return LullabyMediaPlan(key, source_name, str(local_path), kind, sha256_file(local_path), local_path.stat().st_size)


def _validate_image(path: Path, source_name: str) -> None:
    data = path.read_bytes()
    if not data:
        raise StoryValidationError(f"Image is empty: {source_name}")
    suffix = Path(source_name).suffix.lower()
    valid = (suffix in {".jpg", ".jpeg"} and data[:2] == b"\xff\xd8") or \
        (suffix == ".png" and data[:8] == b"\x89PNG\r\n\x1a\n") or \
        (suffix == ".gif" and data[:6] in {b"GIF87a", b"GIF89a"})
    if not valid:
        raise StoryValidationError(f"Image signature does not match {source_name}")


def _extract_single_audio(zip_path: Path, directory: Path, legacy_id: int) -> Path:
    directory.mkdir(parents=True, exist_ok=True)
    try:
        with zipfile.ZipFile(zip_path) as archive:
            files = [item for item in archive.infolist() if not item.is_dir()]
            if len(files) != 1:
                raise StoryValidationError(f"{legacy_id}.zip must contain exactly one audio file")
            member = files[0]
            suffix = Path(member.filename).suffix.lower()
            if suffix != ".mp3":
                raise StoryValidationError(f"{legacy_id}.zip contains unsupported audio type {suffix!r}; only MP3 is accepted")
            if member.file_size > MAX_EXTRACTED_AUDIO_BYTES:
                raise StoryValidationError(
                    f"{legacy_id}.zip MP3 exceeds {MAX_EXTRACTED_AUDIO_BYTES // (1024 * 1024)} MiB extraction limit"
                )
            destination = directory / f"{legacy_id}.mp3"
            with archive.open(member) as source, destination.open("wb") as target:
                extracted = 0
                while block := source.read(1024 * 1024):
                    extracted += len(block)
                    if extracted > MAX_EXTRACTED_AUDIO_BYTES:
                        raise StoryValidationError(
                            f"{legacy_id}.zip MP3 exceeds {MAX_EXTRACTED_AUDIO_BYTES // (1024 * 1024)} MiB extraction limit"
                        )
                    target.write(block)
    except zipfile.BadZipFile as exception:
        raise StoryValidationError(f"{legacy_id}.zip is not a valid ZIP archive") from exception
    if destination.stat().st_size == 0:
        raise StoryValidationError(f"{legacy_id}.zip contains an empty MP3")
    data = destination.read_bytes()
    if not (data.startswith(b"ID3") or any(data[index] == 0xFF and (data[index + 1] & 0xE0) == 0xE0 for index in range(max(0, len(data) - 1)))):
        raise StoryValidationError(f"{legacy_id}.zip does not contain a valid MP3 frame")
    return destination


def _skip_id3v2(data: bytes) -> int:
    if len(data) < 10 or data[:3] != b"ID3":
        return 0
    size = ((data[6] & 0x7F) << 21) | ((data[7] & 0x7F) << 14) | ((data[8] & 0x7F) << 7) | (data[9] & 0x7F)
    return min(len(data), 10 + size + (10 if data[5] & 0x10 else 0))


def _mp3_duration_seconds(path: Path) -> float:
    data = path.read_bytes()
    index = _skip_id3v2(data)
    frames = 0
    samples = 0
    while index + 4 <= len(data):
        header = int.from_bytes(data[index:index + 4], "big")
        parsed = _parse_mp3_header(header)
        if parsed is None:
            index += 1
            continue
        frame_length, sample_count, sample_rate = parsed
        if frame_length <= 0 or index + frame_length > len(data):
            break
        frames += 1
        samples += sample_count
        index += frame_length
    return samples / sample_rate if frames and sample_rate else 0.0


def _parse_mp3_header(header: int) -> tuple[int, int, int] | None:
    if (header >> 21) & 0x7FF != 0x7FF:
        return None
    version = (header >> 19) & 0b11
    layer = (header >> 17) & 0b11
    bitrate_index = (header >> 12) & 0xF
    sample_index = (header >> 10) & 0b11
    padding = (header >> 9) & 1
    if version == 1 or layer != 1 or bitrate_index in {0, 15} or sample_index == 3:
        return None
    version_rates = {3: (44100, 48000, 32000), 2: (22050, 24000, 16000), 0: (11025, 12000, 8000)}
    sample_rate = version_rates.get(version, (0, 0, 0))[sample_index]
    if not sample_rate:
        return None
    bitrates = {
        3: (0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320),
        2: (0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160),
        0: (0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160),
    }
    bitrate = bitrates[version][bitrate_index] * 1000
    sample_count = 1152 if version == 3 else 576
    frame_length = ((144 * bitrate) // sample_rate if version == 3 else (72 * bitrate) // sample_rate) + padding
    return frame_length, sample_count, sample_rate


def _ascii_key(value: str) -> str:
    prepared = value.translate(str.maketrans({"ı": "i", "İ": "I"}))
    return re.sub(r"\s+", " ", unicodedata.normalize("NFKD", prepared).encode("ascii", "ignore").decode("ascii").casefold()).strip()
