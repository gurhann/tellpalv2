#!/usr/bin/env python3
"""Build TellPal V2 content inventory reports from legacy CSV exports."""

from __future__ import annotations

import argparse
import csv
import json
import re
import sys
import unicodedata
from collections import Counter, defaultdict
from dataclasses import dataclass, field
from pathlib import Path
from typing import Iterable


VALID_CONTENT_TYPES = {"STORY", "AUDIO_STORY", "LULLABY", "MEDITATION"}
MIGRATION_CONTENT_TYPES = {"STORY", "LULLABY", "MEDITATION"}
LANGUAGE_CODES = {"tr", "en", "de", "pt", "es"}
DEFAULT_EXCLUSIONS_PATH = Path(__file__).with_name("exclusions.csv")
DRIVE_FOLDER_MIME_TYPE = "application/vnd.google-apps.folder"

TEXT_KEYWORDS = {"metin", "text", "doc", "story", "hikaye", "translation", "çeviri", "ceviri"}
AUDIO_KEYWORDS = {"ses", "audio", "voice", "seslendirme", "mp3", "wav", "m4a"}
ILLUSTRATION_KEYWORDS = {"çizim", "cizim", "illustration", "image", "görsel", "gorsel", "kapak", "cover"}
TEXTLESS_KEYWORDS = {"yazısız", "yazisiz", "textless", "without text"}
REVISION_KEYWORDS = {
    "revize",
    "revision",
    "güncel",
    "guncel",
    "son",
    "final",
    "v2",
    "copy",
    "kopya",
    "hatalı",
    "hatali",
    "eski",
    "old",
    "wrong",
}


@dataclass(frozen=True)
class StoryRow:
    legacy_id: str
    name: str
    author: str
    page_count: int | None
    dubbing: str
    illustrator: str
    duration: int | None
    age_range: int | None
    normalized_name: str


@dataclass(frozen=True)
class CategoryRow:
    language: str
    name: str
    content_type: str
    normalized_name: str


@dataclass(frozen=True)
class StoryCategoryLink:
    language: str
    category_name: str
    story_name: str
    normalized_category_name: str
    normalized_story_name: str


@dataclass
class TypeResolution:
    story: StoryRow
    candidate_languages: set[str] = field(default_factory=set)
    candidate_types: set[str] = field(default_factory=set)
    categories: set[str] = field(default_factory=set)
    final_type: str | None = None
    status: str = "MISSING"
    decision_rule: str = ""
    notes: list[str] = field(default_factory=list)
    canonical_key_value: str = ""
    grouped_legacy_ids: set[str] = field(default_factory=set)

    @property
    def canonical_key(self) -> str:
        if self.canonical_key_value:
            return self.canonical_key_value
        content_type = self.final_type or "UNKNOWN"
        return f"{content_type.lower()}-{self.story.legacy_id}"

    @property
    def canonical_folder(self) -> str:
        content_type = self.final_type or "UNKNOWN"
        return f"{content_type}-{self.canonical_key}-{slugify(self.story.name)}"


@dataclass(frozen=True)
class DriveItem:
    item_id: str
    title: str
    mime_type: str
    url: str
    path: str
    modified_time: str
    normalized_title: str
    normalized_path: str


@dataclass(frozen=True)
class ExclusionRow:
    legacy_id: str
    name: str
    reason: str


def read_csv(path: Path) -> list[dict[str, str]]:
    with path.open("r", encoding="utf-8-sig", newline="") as handle:
        return [{key: normalize_cell(value) for key, value in row.items()} for row in csv.DictReader(handle)]


def normalize_cell(value: str | None) -> str:
    if value is None:
        return ""
    return unicodedata.normalize("NFC", value.strip())


def parse_int(value: str) -> int | None:
    value = value.strip()
    if not value:
        return None
    return int(value)


def normalize_key(value: str) -> str:
    text = unicodedata.normalize("NFKD", value)
    text = "".join(ch for ch in text if not unicodedata.combining(ch))
    text = text.casefold()
    text = re.sub(r"[^\w\s-]", " ", text, flags=re.UNICODE)
    text = re.sub(r"[_\s-]+", " ", text, flags=re.UNICODE).strip()
    return text


def slugify(value: str) -> str:
    text = normalize_key(value)
    text = re.sub(r"[^a-z0-9]+", "-", text)
    text = text.strip("-")
    return text[:80] or "untitled"


def parse_story_rows(path: Path) -> list[StoryRow]:
    rows = read_csv(path)
    required = {"id", "name", "author", "page_count", "dubbing", "illustrator", "duration", "age_range"}
    assert_columns(path, rows, required)
    stories: list[StoryRow] = []
    for row in rows:
        stories.append(
            StoryRow(
                legacy_id=row["id"],
                name=row["name"],
                author=row["author"],
                page_count=parse_int(row["page_count"]),
                dubbing=row["dubbing"],
                illustrator=row["illustrator"],
                duration=parse_int(row["duration"]),
                age_range=parse_int(row["age_range"]),
                normalized_name=normalize_key(row["name"]),
            )
        )
    return stories


def parse_categories(path: Path) -> list[CategoryRow]:
    rows = read_csv(path)
    required = {"language", "name", "type"}
    assert_columns(path, rows, required)
    categories: list[CategoryRow] = []
    invalid: list[str] = []
    for row in rows:
        content_type = row["type"]
        if content_type not in VALID_CONTENT_TYPES:
            invalid.append(f"{row['language']} / {row['name']} / {content_type}")
            continue
        categories.append(
            CategoryRow(
                language=row["language"],
                name=row["name"],
                content_type=content_type,
                normalized_name=normalize_key(row["name"]),
            )
        )
    if invalid:
        joined = "\n".join(invalid)
        raise ValueError(f"Invalid category content types:\n{joined}")
    return categories


def parse_story_category_links(path: Path) -> list[StoryCategoryLink]:
    rows = read_csv(path)
    required = {"language", "category_name", "story_name"}
    assert_columns(path, rows, required)
    return [
        StoryCategoryLink(
            language=row["language"],
            category_name=row["category_name"],
            story_name=row["story_name"],
            normalized_category_name=normalize_key(row["category_name"]),
            normalized_story_name=normalize_key(row["story_name"]),
        )
        for row in rows
    ]


def parse_exclusions(path: Path | None) -> dict[str, ExclusionRow]:
    if path is None or not path.exists():
        return {}
    rows = read_csv(path)
    required = {"legacy_id", "name", "reason"}
    assert_columns(path, rows, required)
    return {
        row["legacy_id"]: ExclusionRow(
            legacy_id=row["legacy_id"],
            name=row["name"],
            reason=row["reason"],
        )
        for row in rows
        if row["legacy_id"]
    }


def assert_columns(path: Path, rows: list[dict[str, str]], required: set[str]) -> None:
    if not rows:
        raise ValueError(f"{path} has no data rows")
    actual = set(rows[0].keys())
    missing = sorted(required - actual)
    if missing:
        raise ValueError(f"{path} missing required columns: {', '.join(missing)}")


def resolve_content_types(
    stories: list[StoryRow],
    categories: list[CategoryRow],
    links: list[StoryCategoryLink],
    exclusions: dict[str, ExclusionRow],
) -> tuple[list[TypeResolution], list[dict[str, str]]]:
    category_by_exact = {(c.language, c.name): c for c in categories}
    category_by_normalized = {(c.language, c.normalized_name): c for c in categories}
    missing_category_links: list[dict[str, str]] = []
    candidates_by_story_name: dict[str, list[tuple[StoryCategoryLink, CategoryRow]]] = defaultdict(list)

    for link in links:
        category = category_by_exact.get((link.language, link.category_name))
        if category is None:
            category = category_by_normalized.get((link.language, link.normalized_category_name))
        if category is None:
            missing_category_links.append(
                {
                    "language": link.language,
                    "category_name": link.category_name,
                    "story_name": link.story_name,
                }
            )
            continue
        candidates_by_story_name[link.normalized_story_name].append((link, category))

    resolutions: list[TypeResolution] = []
    for story in stories:
        resolution = TypeResolution(story=story)
        for link, category in candidates_by_story_name.get(story.normalized_name, []):
            resolution.candidate_languages.add(link.language)
            resolution.candidate_types.add(category.content_type)
            resolution.categories.add(f"{link.language}:{category.name}")
        choose_final_type(resolution)
        apply_scope_rules(resolution)
        apply_exclusion(resolution, exclusions)
        resolutions.append(resolution)

    assign_canonical_keys(resolutions)
    return resolutions, missing_category_links


def apply_exclusion(resolution: TypeResolution, exclusions: dict[str, ExclusionRow]) -> None:
    exclusion = exclusions.get(resolution.story.legacy_id)
    if exclusion is None:
        return
    resolution.final_type = None
    resolution.status = "OUT_OF_SCOPE"
    resolution.decision_rule = "manual_scope_exclusion"
    resolution.notes.append(exclusion.reason)


def apply_scope_rules(resolution: TypeResolution) -> None:
    if resolution.final_type in MIGRATION_CONTENT_TYPES:
        return
    if resolution.final_type == "AUDIO_STORY":
        resolution.final_type = None
        resolution.status = "OUT_OF_SCOPE"
        resolution.decision_rule = "audio_story_scope_exclusion"
        resolution.notes.append("AUDIO_STORY is out of scope; audio stories are covered by STORY migration")


def assign_canonical_keys(resolutions: list[TypeResolution]) -> None:
    groups: dict[tuple[str, str], list[TypeResolution]] = defaultdict(list)
    for resolution in resolutions:
        if not resolution.final_type:
            resolution.canonical_key_value = f"unknown-{resolution.story.legacy_id}"
            continue
        groups[(resolution.final_type, resolution.story.normalized_name)].append(resolution)

    used: set[str] = set()
    for (content_type, _), group in sorted(groups.items(), key=lambda item: item[0]):
        base = f"{content_type.lower()}-{slugify(group[0].story.name)}"
        key = base
        if key in used:
            ids = "-".join(sorted(row.story.legacy_id for row in group)[:3])
            key = f"{base}-{ids}"
        used.add(key)
        legacy_ids = {row.story.legacy_id for row in group}
        for resolution in group:
            resolution.canonical_key_value = key
            resolution.grouped_legacy_ids = legacy_ids


def choose_final_type(resolution: TypeResolution) -> None:
    types = resolution.candidate_types
    page_count = resolution.story.page_count

    if "MEDITATION" in types and "LULLABY" in types:
        resolution.status = "TYPE_CONFLICT"
        resolution.decision_rule = "meditation_lullaby_conflict"
        resolution.notes.append("MEDITATION and LULLABY category types both found")
        return

    if "MEDITATION" in types:
        resolution.final_type = "MEDITATION"
        resolution.status = "MATCHED"
        resolution.decision_rule = "category_meditation"
        add_page_count_note(resolution)
        return

    if "LULLABY" in types:
        resolution.final_type = "LULLABY"
        resolution.status = "MATCHED"
        resolution.decision_rule = "category_lullaby"
        add_page_count_note(resolution)
        return

    if page_count is None:
        if types == {"STORY"}:
            resolution.final_type = "STORY"
            resolution.status = "LIKELY"
            resolution.decision_rule = "category_story_page_count_missing"
        elif types == {"AUDIO_STORY"}:
            resolution.final_type = "AUDIO_STORY"
            resolution.status = "LIKELY"
            resolution.decision_rule = "category_audio_story_page_count_missing"
        elif types:
            resolution.status = "TYPE_CONFLICT"
            resolution.decision_rule = "page_count_missing_type_conflict"
            resolution.notes.append("page_count missing and category types conflict")
        else:
            resolution.status = "MISSING"
            resolution.decision_rule = "no_category_or_page_count"
        return

    if page_count > 1:
        resolution.final_type = "STORY"
        resolution.decision_rule = "page_count_gt_1_story"
        if "AUDIO_STORY" in types and "STORY" in types:
            resolution.status = "MATCHED"
            resolution.notes.append("AUDIO_STORY/STORY category conflict resolved by page_count")
        elif "AUDIO_STORY" in types and "STORY" not in types:
            resolution.status = "TYPE_CONFLICT"
            resolution.notes.append("page_count > 1 overrides AUDIO_STORY-only category signal")
        else:
            resolution.status = "MATCHED" if types else "LIKELY"
        return

    resolution.final_type = "AUDIO_STORY"
    resolution.decision_rule = "page_count_1_default_audio_story"
    if "STORY" in types and "AUDIO_STORY" in types:
        resolution.status = "MATCHED"
        resolution.notes.append("AUDIO_STORY/STORY category conflict resolved by page_count")
    elif "STORY" in types and "AUDIO_STORY" not in types:
        resolution.status = "TYPE_CONFLICT"
        resolution.notes.append("page_count = 1 overrides STORY-only category signal")
    elif "AUDIO_STORY" in types:
        resolution.status = "MATCHED"
    else:
        resolution.status = "LIKELY"


def add_page_count_note(resolution: TypeResolution) -> None:
    if resolution.story.page_count and resolution.story.page_count > 1:
        resolution.notes.append("non-story type has page_count > 1")
        resolution.status = "TYPE_CONFLICT"


def load_drive_inventory(path: Path | None) -> list[DriveItem]:
    if path is None:
        return []
    items: list[DriveItem] = []
    with path.open("r", encoding="utf-8-sig") as handle:
        for line_number, line in enumerate(handle, start=1):
            if not line.strip():
                continue
            raw = json.loads(line)
            title = str(raw.get("title") or raw.get("name") or raw.get("display_title") or "")
            item_path = str(raw.get("path") or raw.get("drive_path") or raw.get("parent_path") or "")
            items.append(
                DriveItem(
                    item_id=str(raw.get("id") or raw.get("file_id") or raw.get("item_id") or f"line-{line_number}"),
                    title=title,
                    mime_type=str(raw.get("mime_type") or raw.get("mimeType") or ""),
                    url=str(raw.get("url") or raw.get("web_url") or raw.get("display_url") or ""),
                    path=item_path,
                    modified_time=str(raw.get("modified_time") or raw.get("modifiedTime") or ""),
                    normalized_title=normalize_key(title),
                    normalized_path=normalize_key(f"{item_path} {title}"),
                )
            )
    return items


def build_drive_matches(
    resolutions: list[TypeResolution],
    drive_items: list[DriveItem],
    include_likely_in_copy_plan: bool = False,
) -> tuple[list[dict[str, str]], list[dict[str, str]]]:
    report_rows: list[dict[str, str]] = []
    copy_plan: list[dict[str, str]] = []

    for resolution in resolutions:
        if resolution.status == "OUT_OF_SCOPE":
            continue
        story = resolution.story
        scored = sorted(
            (
                (score_drive_item(story, item), item)
                for item in drive_items
            ),
            key=lambda pair: pair[0],
            reverse=True,
        )
        candidates = [(score, item) for score, item in scored if score >= 55][:10]
        if not candidates:
            report_rows.append(drive_report_row(resolution, None, 0, "MISSING"))
            continue

        top_score = candidates[0][0]
        top_count = sum(1 for score, _ in candidates if score == top_score)
        status = "MATCHED" if top_score >= 90 and top_count == 1 else "LIKELY" if top_score >= 70 else "AMBIGUOUS"
        if top_count > 1 and top_score < 100:
            status = "AMBIGUOUS"

        for score, item in candidates:
            row_status = status if score == top_score else "CANDIDATE"
            report_rows.append(drive_report_row(resolution, item, score, row_status))
            if row_status == "MATCHED" or (include_likely_in_copy_plan and row_status == "LIKELY"):
                copy_plan.extend(build_copy_plan_entries(resolution, item, drive_items))

    return report_rows, copy_plan


def score_drive_item(story: StoryRow, item: DriveItem) -> int:
    if not story.normalized_name:
        return 0
    haystack = item.normalized_path
    score = 0
    if story.normalized_name == item.normalized_title:
        score += 100
    elif story.normalized_name in haystack:
        score += 85
    else:
        tokens = [token for token in story.normalized_name.split() if len(token) > 2]
        if tokens:
            matched = sum(1 for token in tokens if token in haystack)
            score += int((matched / len(tokens)) * 70)
    if story.legacy_id and re.search(rf"(^|\D){re.escape(story.legacy_id)}(\D|$)", haystack):
        score += 10
    return min(score, 100)


def classify_drive_item(item: DriveItem) -> str:
    haystack = item.normalized_path
    mime = item.mime_type.casefold()
    if contains_any(haystack, TEXTLESS_KEYWORDS):
        return "textless"
    if contains_any(haystack, REVISION_KEYWORDS):
        return "revision"
    if "image" in mime or contains_any(haystack, ILLUSTRATION_KEYWORDS):
        return "illustration"
    if "document" in mime or "text" in mime or contains_any(haystack, TEXT_KEYWORDS):
        return "text"
    if "audio" in mime or contains_any(haystack, AUDIO_KEYWORDS):
        return "audio"
    return "other"


def contains_any(value: str, keywords: Iterable[str]) -> bool:
    normalized_keywords = {normalize_key(keyword) for keyword in keywords}
    return any(keyword in value for keyword in normalized_keywords)


def drive_report_row(
    resolution: TypeResolution,
    item: DriveItem | None,
    score: int,
    status: str,
) -> dict[str, str]:
    return {
        "legacy_id": resolution.story.legacy_id,
        "name": resolution.story.name,
        "content_type": resolution.final_type or "",
        "canonical_key": resolution.canonical_key,
        "status": status,
        "score": str(score),
        "asset_role": classify_drive_item(item) if item else "",
        "drive_id": item.item_id if item else "",
        "drive_title": item.title if item else "",
        "drive_path": item.path if item else "",
        "mime_type": item.mime_type if item else "",
        "modified_time": item.modified_time if item else "",
        "url": item.url if item else "",
    }


def build_copy_plan_entries(
    resolution: TypeResolution,
    item: DriveItem,
    drive_items: list[DriveItem],
) -> list[dict[str, str]]:
    if item.mime_type != DRIVE_FOLDER_MIME_TYPE:
        return [build_copy_plan_entry(resolution, item)]

    descendant_prefix = f"{item.path.rstrip('/')}/"
    descendants = [
        descendant
        for descendant in drive_items
        if descendant.mime_type != DRIVE_FOLDER_MIME_TYPE
        and descendant.path.startswith(descendant_prefix)
    ]
    return [build_copy_plan_entry(resolution, descendant) for descendant in descendants]


def build_copy_plan_entry(resolution: TypeResolution, item: DriveItem) -> dict[str, str]:
    role = classify_drive_item(item)
    language = infer_language(item.path) or first_language(resolution) or "und"
    target_folder = target_subfolder(resolution.final_type, role, language)
    return {
        "source_drive_id": item.item_id,
        "source_url": item.url,
        "source_title": item.title,
        "source_path": item.path,
        "canonical_key": resolution.canonical_key,
        "target_content_folder": resolution.canonical_folder,
        "target_subfolder": target_folder,
        "asset_role": role,
        "language": language,
        "copy_mode": "copy",
    }


def target_subfolder(content_type: str | None, role: str, language: str) -> str:
    if role == "textless" and content_type == "STORY":
        return "02_shared/textless"
    if role == "revision":
        return "03_revisions"
    if role == "text":
        return f"01_localizations/{language}/texts"
    if role == "audio":
        return f"01_localizations/{language}/audio"
    if role == "illustration":
        return f"01_localizations/{language}/illustrations"
    return "04_other"


def infer_language(value: str) -> str:
    normalized = normalize_key(value)
    language_markers = {
        "tr": {"tr", "turkce", "türkçe", "turkish"},
        "en": {"en", "eng", "ing", "ingilizce", "english"},
        "de": {"de", "ger", "almanca", "german", "deutsch"},
        "pt": {"pt", "ptbr", "portekizce", "portuguese", "portugues"},
        "es": {"es", "ispanyolca", "spanish", "espanol"},
    }
    for code, markers in language_markers.items():
        if any(normalize_key(marker) in normalized.split() or normalize_key(marker) in normalized for marker in markers):
            return code
    return ""


def first_language(resolution: TypeResolution) -> str:
    languages = sorted(lang for lang in resolution.candidate_languages if lang in LANGUAGE_CODES)
    return languages[0] if languages else ""


def write_csv(path: Path, rows: list[dict[str, str]], fieldnames: list[str]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=fieldnames, extrasaction="ignore")
        writer.writeheader()
        writer.writerows(rows)


def write_jsonl(path: Path, rows: list[dict[str, str]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8") as handle:
        for row in rows:
            handle.write(json.dumps(row, ensure_ascii=False, sort_keys=True))
            handle.write("\n")


def write_reports(
    output_dir: Path,
    categories: list[CategoryRow],
    links: list[StoryCategoryLink],
    resolutions: list[TypeResolution],
    missing_category_links: list[dict[str, str]],
    drive_report: list[dict[str, str]],
    copy_plan: list[dict[str, str]],
) -> dict[str, object]:
    category_rows = [
        {
            "language": c.language,
            "name": c.name,
            "normalized_name": c.normalized_name,
            "type": c.content_type,
        }
        for c in categories
    ]
    write_csv(output_dir / "category_type_map.csv", category_rows, ["language", "name", "normalized_name", "type"])

    resolution_rows = [resolution_row(resolution) for resolution in resolutions]
    write_csv(
        output_dir / "content_type_resolution_report.csv",
        resolution_rows,
        [
            "legacy_id",
            "name",
            "page_count",
            "candidate_languages",
            "candidate_types",
            "final_type",
            "status",
            "decision_rule",
            "categories",
            "notes",
        ],
    )

    import_rows = [
        import_manifest_row(resolution)
        for resolution in resolutions
        if resolution.final_type and resolution.status != "OUT_OF_SCOPE"
    ]
    write_csv(
        output_dir / "v2_import_manifest.csv",
        import_rows,
        [
            "external_key",
            "legacy_ids",
            "content_type",
            "canonical_title",
            "languages",
            "category_links",
            "title",
            "page_count",
            "duration_minutes",
            "age_range",
            "author",
            "dubbing",
            "illustrator",
            "resolution_status",
            "target_folder",
        ],
    )

    write_csv(
        output_dir / "match_report.csv",
        drive_report,
        [
            "legacy_id",
            "name",
            "content_type",
            "canonical_key",
            "status",
            "score",
            "asset_role",
            "drive_id",
            "drive_title",
            "drive_path",
            "mime_type",
            "modified_time",
            "url",
        ],
    )
    write_jsonl(output_dir / "drive_copy_plan.jsonl", copy_plan)
    write_jsonl(output_dir / "missing_category_links.jsonl", missing_category_links)

    summary = build_summary(categories, links, resolutions, missing_category_links, drive_report, copy_plan)
    with (output_dir / "summary.json").open("w", encoding="utf-8") as handle:
        json.dump(summary, handle, ensure_ascii=False, indent=2, sort_keys=True)
        handle.write("\n")
    return summary


def resolution_row(resolution: TypeResolution) -> dict[str, str]:
    story = resolution.story
    return {
        "legacy_id": story.legacy_id,
        "name": story.name,
        "page_count": "" if story.page_count is None else str(story.page_count),
        "candidate_languages": "|".join(sorted(resolution.candidate_languages)),
        "candidate_types": "|".join(sorted(resolution.candidate_types)),
        "final_type": resolution.final_type or "",
        "status": resolution.status,
        "decision_rule": resolution.decision_rule,
        "categories": "|".join(sorted(resolution.categories)),
        "notes": "|".join(resolution.notes),
    }


def import_manifest_row(resolution: TypeResolution) -> dict[str, str]:
    story = resolution.story
    return {
        "external_key": f"legacy-content-{resolution.canonical_key}",
        "legacy_ids": "|".join(sorted(resolution.grouped_legacy_ids or {story.legacy_id})),
        "content_type": resolution.final_type or "",
        "canonical_title": story.name,
        "languages": "|".join(sorted(resolution.candidate_languages)) or "und",
        "category_links": "|".join(sorted(resolution.categories)),
        "title": story.name,
        "page_count": "" if story.page_count is None else str(story.page_count),
        "duration_minutes": "" if story.duration is None else str(story.duration),
        "age_range": "" if story.age_range is None else str(story.age_range),
        "author": story.author,
        "dubbing": story.dubbing,
        "illustrator": story.illustrator,
        "resolution_status": resolution.status,
        "target_folder": resolution.canonical_folder,
    }


def build_summary(
    categories: list[CategoryRow],
    links: list[StoryCategoryLink],
    resolutions: list[TypeResolution],
    missing_category_links: list[dict[str, str]],
    drive_report: list[dict[str, str]],
    copy_plan: list[dict[str, str]],
) -> dict[str, object]:
    final_type_counts = Counter(r.final_type or "UNRESOLVED" for r in resolutions)
    status_counts = Counter(r.status for r in resolutions)
    out_of_scope_rule_counts = Counter(r.decision_rule for r in resolutions if r.status == "OUT_OF_SCOPE")
    drive_status_counts = Counter(row["status"] for row in drive_report)
    category_type_counts = Counter(c.content_type for c in categories)
    canonical_keys = {r.canonical_key for r in resolutions if r.final_type and r.status != "OUT_OF_SCOPE"}
    return {
        "categories": len(categories),
        "category_types": dict(sorted(category_type_counts.items())),
        "canonical_content_count": len(canonical_keys),
        "story_category_links": len(links),
        "missing_category_links": len(missing_category_links),
        "inventory_rows": len(resolutions),
        "content_type_counts": dict(sorted(final_type_counts.items())),
        "resolution_status_counts": dict(sorted(status_counts.items())),
        "out_of_scope_decision_rules": dict(sorted(out_of_scope_rule_counts.items())),
        "drive_match_status_counts": dict(sorted(drive_status_counts.items())),
        "drive_copy_plan_entries": len(copy_plan),
    }


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--stories", type=Path, required=True, help="Path to tellpal_stories.csv")
    parser.add_argument("--story-categories", type=Path, required=True, help="Path to stories_and_categories.csv")
    parser.add_argument("--categories", type=Path, required=True, help="Path to categories.csv")
    parser.add_argument("--drive-inventory-cache", type=Path, help="Optional JSONL Drive metadata cache")
    parser.add_argument(
        "--exclusions",
        type=Path,
        default=DEFAULT_EXCLUSIONS_PATH,
        help="Optional CSV of legacy ids to keep out of scope",
    )
    parser.add_argument("--output-dir", type=Path, default=Path("ops/content-migration/out"))
    parser.add_argument(
        "--include-likely-in-copy-plan",
        action="store_true",
        help="Include LIKELY matches in drive_copy_plan.jsonl after manual review",
    )
    parser.add_argument("--execute", action="store_true", help="Reserved for future Drive copy execution")
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv or sys.argv[1:])
    if args.execute:
        raise SystemExit(
            "--execute is intentionally not implemented yet. Generate and review drive_copy_plan.jsonl, "
            "then add a Drive API copy adapter with explicit credentials."
        )

    stories = parse_story_rows(args.stories)
    categories = parse_categories(args.categories)
    links = parse_story_category_links(args.story_categories)
    exclusions = parse_exclusions(args.exclusions)
    resolutions, missing_category_links = resolve_content_types(stories, categories, links, exclusions)
    drive_items = load_drive_inventory(args.drive_inventory_cache)
    drive_report, copy_plan = build_drive_matches(
        resolutions,
        drive_items,
        include_likely_in_copy_plan=args.include_likely_in_copy_plan,
    )
    summary = write_reports(
        args.output_dir,
        categories,
        links,
        resolutions,
        missing_category_links,
        drive_report,
        copy_plan,
    )
    print(json.dumps(summary, ensure_ascii=True, indent=2, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
