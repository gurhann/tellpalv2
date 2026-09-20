from __future__ import annotations

from dataclasses import dataclass
from datetime import UTC, datetime
from typing import Any

from category_story_mapping_manifest import (
    CategoryStoryLanePlan,
    CategoryStoryMappingPlan,
    CategoryStoryMappingRow,
    assert_source_unchanged,
    normalize_key,
)
from tellpal_admin_client import TellPalAdminClient


class CategoryStoryMappingImportError(RuntimeError):
    """Raised when the remote mapping state is not deterministic or verifiable."""


@dataclass(frozen=True)
class MappingConflict:
    code: str
    message: str
    language_code: str | None = None
    category_name: str | None = None
    story_title: str | None = None
    source_row_numbers: tuple[int, ...] = ()

    def to_dict(self) -> dict[str, object]:
        return {
            "code": self.code,
            "message": self.message,
            "languageCode": self.language_code,
            "categoryName": self.category_name,
            "storyTitle": self.story_title,
            "sourceRowNumbers": list(self.source_row_numbers),
        }


@dataclass(frozen=True)
class RemoteMappingAssignment:
    row: CategoryStoryMappingRow
    category_id: int
    content_id: int
    display_order: int
    action: str
    existing: dict[str, object] | None = None

    def to_dict(self) -> dict[str, object]:
        return {
            "sourceRowNumber": self.row.source_row_number,
            "languageCode": self.row.language_code,
            "categoryName": self.row.category_name,
            "storyTitle": self.row.story_title,
            "categoryId": self.category_id,
            "contentId": self.content_id,
            "displayOrder": self.display_order,
            "action": self.action,
        }


@dataclass(frozen=True)
class RemoteMappingLane:
    source: CategoryStoryLanePlan
    category_id: int
    category_localization: dict[str, object]
    category_active: bool
    publish_category_localization: bool
    current_entries: tuple[dict[str, object], ...]
    assignments: tuple[RemoteMappingAssignment, ...]

    @property
    def created_count(self) -> int:
        return sum(item.action == "CREATE" for item in self.assignments)

    @property
    def reused_count(self) -> int:
        return sum(item.action == "REUSE" for item in self.assignments)

    def to_dict(self) -> dict[str, object]:
        return {
            "languageCode": self.source.language_code,
            "categoryName": self.source.category_name,
            "canonicalType": self.source.canonical_type,
            "categoryId": self.category_id,
            "categoryLocalizationStatus": self.category_localization.get("status"),
            "categoryLocalizationAction": "PUBLISH"
            if self.publish_category_localization
            else "REUSE",
            "currentEntryCount": len(self.current_entries),
            "createdCount": self.created_count,
            "reusedCount": self.reused_count,
            "assignments": [item.to_dict() for item in self.assignments],
        }


@dataclass(frozen=True)
class RemoteMappingPlan:
    lanes: tuple[RemoteMappingLane, ...]
    conflicts: tuple[MappingConflict, ...]

    @property
    def ready(self) -> bool:
        return not self.conflicts

    @property
    def lane_count(self) -> int:
        return len(self.lanes)

    @property
    def publish_count(self) -> int:
        return sum(lane.publish_category_localization for lane in self.lanes)

    @property
    def create_count(self) -> int:
        return sum(lane.created_count for lane in self.lanes)

    @property
    def reuse_count(self) -> int:
        return sum(lane.reused_count for lane in self.lanes)

    def to_dict(self) -> dict[str, object]:
        return {
            "ready": self.ready,
            "laneCount": self.lane_count,
            "publishCategoryLocalizationCount": self.publish_count,
            "createCount": self.create_count,
            "reuseCount": self.reuse_count,
            "conflicts": [item.to_dict() for item in self.conflicts],
            "lanes": [lane.to_dict() for lane in self.lanes],
        }


def remote_preflight(
    plan: CategoryStoryMappingPlan,
    client: TellPalAdminClient,
) -> RemoteMappingPlan:
    """Read and reconcile all remote state without performing a mutation."""
    categories = client.list_categories()
    category_candidates = _index_category_candidates(categories, client)
    content_candidates = _index_content_candidates(client.list_contents())
    lanes: list[RemoteMappingLane] = []
    conflicts: list[MappingConflict] = []

    for source_lane in plan.lanes:
        category_matches = category_candidates.get(
            (source_lane.language_code, source_lane.normalized_category_name), []
        )
        category_id, category, localization = _resolve_category(
            source_lane, category_matches, conflicts
        )
        if category_id is None or category is None or localization is None:
            continue

        if category.get("type") != source_lane.canonical_type:
            conflicts.append(
                MappingConflict(
                    code="category_type_mismatch",
                    message=(
                        f"Category {source_lane.category_name!r} has remote type "
                        f"{category.get('type')!r}, expected {source_lane.canonical_type!r}"
                    ),
                    language_code=source_lane.language_code,
                    category_name=source_lane.category_name,
                    source_row_numbers=tuple(row.source_row_number for row in source_lane.rows),
                )
            )
            continue
        if category.get("active") is not True:
            conflicts.append(
                MappingConflict(
                    code="inactive_category",
                    message=f"Category {source_lane.category_name!r} is inactive",
                    language_code=source_lane.language_code,
                    category_name=source_lane.category_name,
                    source_row_numbers=tuple(row.source_row_number for row in source_lane.rows),
                )
            )
            continue

        status = str(localization.get("status") or "").upper()
        publish_category_localization = status == "DRAFT"
        if status not in {"DRAFT", "PUBLISHED"}:
            conflicts.append(
                MappingConflict(
                    code="unsupported_category_localization_status",
                    message=(
                        f"Category localization {source_lane.category_name!r}/"
                        f"{source_lane.language_code} has unsupported status {status!r}"
                    ),
                    language_code=source_lane.language_code,
                    category_name=source_lane.category_name,
                    source_row_numbers=tuple(row.source_row_number for row in source_lane.rows),
                )
            )
            continue

        current_entries = tuple(client.list_category_contents(category_id, source_lane.language_code))
        lane_assignments: list[RemoteMappingAssignment] = []
        lane_conflicts: list[MappingConflict] = []
        for display_order, row in enumerate(source_lane.rows):
            candidates = content_candidates.get(row.content_key, [])
            content = _resolve_content(row, candidates, lane_conflicts)
            if content is None:
                continue
            content_id = _required_id(content, "contentId")
            lane_assignments.append(
                RemoteMappingAssignment(
                    row=row,
                    category_id=category_id,
                    content_id=content_id,
                    display_order=display_order,
                    action="CREATE",
                )
            )

        existing_by_content: dict[int, dict[str, object]] = {}
        existing_by_order: dict[int, dict[str, object]] = {}
        for entry in current_entries:
            try:
                content_id = _required_id(entry, "contentId")
                display_order = _required_non_negative_int(entry, "displayOrder")
            except CategoryStoryMappingImportError as exception:
                lane_conflicts.append(
                    MappingConflict(
                        code="invalid_existing_curation",
                        message=str(exception),
                        language_code=source_lane.language_code,
                        category_name=source_lane.category_name,
                    )
                )
                continue
            if content_id in existing_by_content:
                lane_conflicts.append(
                    MappingConflict(
                        code="duplicate_existing_curation_content",
                        message=f"Content {content_id} appears more than once in existing curation",
                        language_code=source_lane.language_code,
                        category_name=source_lane.category_name,
                    )
                )
            if display_order in existing_by_order:
                lane_conflicts.append(
                    MappingConflict(
                        code="duplicate_existing_curation_order",
                        message=f"Display order {display_order} appears more than once in existing curation",
                        language_code=source_lane.language_code,
                        category_name=source_lane.category_name,
                    )
                )
            existing_by_content[content_id] = entry
            existing_by_order[display_order] = entry

        planned_content_ids = {item.content_id for item in lane_assignments}
        for existing_content_id in sorted(set(existing_by_content) - planned_content_ids):
            lane_conflicts.append(
                MappingConflict(
                    code="unexpected_existing_curation",
                    message=(
                        f"Existing content {existing_content_id} is not present in the source lane; "
                        "refusing to alter editorial state"
                    ),
                    language_code=source_lane.language_code,
                    category_name=source_lane.category_name,
                )
            )

        seen_planned_content: set[int] = set()
        for assignment in lane_assignments:
            if assignment.content_id in seen_planned_content:
                lane_conflicts.append(
                    MappingConflict(
                        code="duplicate_canonical_content_in_lane",
                        message=(
                            f"Content {assignment.content_id} resolves more than once in the same lane; "
                            "source STORY/AUDIO_STORY overlap must be reviewed"
                        ),
                        language_code=source_lane.language_code,
                        category_name=source_lane.category_name,
                        story_title=assignment.row.story_title,
                        source_row_numbers=(assignment.row.source_row_number,),
                    )
                )
            seen_planned_content.add(assignment.content_id)
            existing = existing_by_content.get(assignment.content_id)
            if existing is not None:
                existing_order = _required_non_negative_int(existing, "displayOrder")
                if existing_order != assignment.display_order:
                    lane_conflicts.append(
                        MappingConflict(
                            code="existing_order_conflict",
                            message=(
                                f"Content {assignment.content_id} already has display order "
                                f"{existing_order}, source expects {assignment.display_order}"
                            ),
                            language_code=source_lane.language_code,
                            category_name=source_lane.category_name,
                            story_title=assignment.row.story_title,
                            source_row_numbers=(assignment.row.source_row_number,),
                        )
                    )
                lane_assignments[lane_assignments.index(assignment)] = RemoteMappingAssignment(
                    row=assignment.row,
                    category_id=assignment.category_id,
                    content_id=assignment.content_id,
                    display_order=assignment.display_order,
                    action="REUSE",
                    existing=existing,
                )
            elif assignment.display_order in existing_by_order:
                occupying = _required_id(existing_by_order[assignment.display_order], "contentId")
                if occupying != assignment.content_id:
                    lane_conflicts.append(
                        MappingConflict(
                            code="existing_order_conflict",
                            message=(
                                f"Source display order {assignment.display_order} is occupied by "
                                f"content {occupying}, not {assignment.content_id}"
                            ),
                            language_code=source_lane.language_code,
                            category_name=source_lane.category_name,
                            story_title=assignment.row.story_title,
                            source_row_numbers=(assignment.row.source_row_number,),
                        )
                    )

        conflicts.extend(lane_conflicts)
        lanes.append(
            RemoteMappingLane(
                source=source_lane,
                category_id=category_id,
                category_localization=localization,
                category_active=True,
                publish_category_localization=publish_category_localization,
                current_entries=current_entries,
                assignments=tuple(lane_assignments),
            )
        )

    return RemoteMappingPlan(lanes=tuple(lanes), conflicts=tuple(conflicts))


def execute_mapping_import(
    plan: CategoryStoryMappingPlan,
    client: TellPalAdminClient,
    report: Any,
    remote: RemoteMappingPlan,
) -> dict[str, int]:
    """Publish approved category localizations, add missing links, then verify every lane."""
    assert_source_unchanged(plan)
    require_ready(remote)
    report.mark_running()

    published_count = 0
    created_count = 0
    reused_count = 0
    for lane in remote.lanes:
        if lane.publish_category_localization:
            localization = lane.category_localization
            response = client.update_category_localization(
                lane.category_id,
                lane.source.language_code,
                {
                    "name": localization.get("name"),
                    "description": localization.get("description"),
                    "imageMediaId": localization.get("imageMediaId"),
                    "status": "PUBLISHED",
                    "publishedAt": utc_now(),
                },
            )
            del response
            published_count += 1
            report.record_category_localization(
                lane.category_id,
                lane.source.category_name,
                lane.source.language_code,
                "PUBLISH",
            )

        for assignment in lane.assignments:
            if assignment.action == "REUSE":
                reused_count += 1
                report.record_link(assignment, "REUSE")
                continue
            client.add_category_content(
                lane.category_id,
                lane.source.language_code,
                {"contentId": assignment.content_id, "displayOrder": assignment.display_order},
            )
            created_count += 1
            report.record_link(assignment, "CREATE")

    report.record_step("mutations-complete")
    for lane in remote.lanes:
        _verify_lane(lane, client)
    report.record_step("verification-complete")
    summary = {
        "lanes": len(remote.lanes),
        "publishedCategoryLocalizations": published_count,
        "createdLinks": created_count,
        "reusedLinks": reused_count,
    }
    report.mark_success(summary)
    return summary


def require_ready(remote: RemoteMappingPlan) -> None:
    if remote.conflicts:
        lines = ["Remote preflight is blocked:"]
        lines.extend(f"- [{item.code}] {item.message}" for item in remote.conflicts)
        raise CategoryStoryMappingImportError("\n".join(lines))


def format_preview(
    plan: CategoryStoryMappingPlan,
    remote: RemoteMappingPlan | None = None,
) -> str:
    lines = [
        "Category/story mapping preview",
        f"CSV rows: {plan.logical_row_count}",
        f"Unique assignments: {plan.unique_assignment_count}",
        f"Exact duplicate rows skipped: {plan.duplicate_count}",
        f"Languages: {', '.join(plan.languages)}",
        f"Category labels: {plan.category_label_count}",
        f"Source many-to-many title risks: {plan.content_candidate_risk_count}",
        f"Source fingerprint: {plan.source_fingerprint}",
    ]
    if remote is None:
        return "\n".join(lines)
    lines.extend(
        [
            f"Remote lanes: {remote.lane_count}",
            f"Category localizations to publish: {remote.publish_count}",
            f"Links to create: {remote.create_count}",
            f"Links to reuse: {remote.reuse_count}",
            f"Conflicts: {len(remote.conflicts)}",
        ]
    )
    for conflict in remote.conflicts[:20]:
        lines.append(f"  - [{conflict.code}] {conflict.message}")
    if len(remote.conflicts) > 20:
        lines.append(f"  - ... {len(remote.conflicts) - 20} more conflicts")
    return "\n".join(lines)


def _index_category_candidates(
    categories: list[dict[str, object]],
    client: TellPalAdminClient,
) -> dict[tuple[str, str], list[tuple[int, dict[str, object], dict[str, object]]]]:
    indexed: dict[tuple[str, str], list[tuple[int, dict[str, object], dict[str, object]]]] = {}
    for category in categories:
        category_id = _required_id(category, "categoryId")
        for localization in client.list_category_localizations(category_id):
            language = normalize_key(localization.get("languageCode"))
            name = normalize_key(localization.get("name"))
            if not language or not name:
                continue
            indexed.setdefault((language, name), []).append((category_id, category, localization))
    return indexed


def _index_content_candidates(
    contents: list[dict[str, object]],
) -> dict[tuple[str, str, str], list[dict[str, object]]]:
    indexed: dict[tuple[str, str, str], list[dict[str, object]]] = {}
    for content in contents:
        content_type = str(content.get("type") or "").upper()
        canonical_type = "STORY" if content_type == "AUDIO_STORY" else content_type
        if canonical_type not in {"STORY", "MEDITATION", "LULLABY"}:
            continue
        try:
            _required_id(content, "contentId")
        except CategoryStoryMappingImportError:
            continue
        for localization in content.get("localizations") or []:
            if not isinstance(localization, dict):
                continue
            language = normalize_key(localization.get("languageCode"))
            title = normalize_key(localization.get("title"))
            if not language or not title:
                continue
            indexed.setdefault((language, canonical_type, title), []).append(
                {"content": content, "localization": localization}
            )
    return indexed


def _resolve_category(
    source_lane: CategoryStoryLanePlan,
    candidates: list[tuple[int, dict[str, object], dict[str, object]]],
    conflicts: list[MappingConflict],
) -> tuple[int | None, dict[str, object] | None, dict[str, object] | None]:
    category_ids = {candidate[0] for candidate in candidates}
    if not candidates:
        conflicts.append(
            MappingConflict(
                code="missing_category_localization",
                message=(
                    f"No category localization matches {source_lane.category_name!r}/"
                    f"{source_lane.language_code}"
                ),
                language_code=source_lane.language_code,
                category_name=source_lane.category_name,
                source_row_numbers=tuple(row.source_row_number for row in source_lane.rows),
            )
        )
        return None, None, None
    if len(category_ids) != 1:
        conflicts.append(
            MappingConflict(
                code="ambiguous_category_localization",
                message=(
                    f"Category localization {source_lane.category_name!r}/"
                    f"{source_lane.language_code} matches category IDs {sorted(category_ids)}"
                ),
                language_code=source_lane.language_code,
                category_name=source_lane.category_name,
                source_row_numbers=tuple(row.source_row_number for row in source_lane.rows),
            )
        )
        return None, None, None
    category_id, category, localization = candidates[0]
    return category_id, category, localization


def _resolve_content(
    row: CategoryStoryMappingRow,
    candidates: list[dict[str, object]],
    conflicts: list[MappingConflict],
) -> dict[str, object] | None:
    candidate_by_id: dict[int, dict[str, object]] = {}
    for candidate in candidates:
        content = candidate["content"]
        if not isinstance(content, dict):
            continue
        candidate_by_id[_required_id(content, "contentId")] = candidate
    if not candidate_by_id:
        conflicts.append(
            MappingConflict(
                code="missing_content_localization",
                message=f"No content matches {row.story_title!r}/{row.language_code}/{row.canonical_type}",
                language_code=row.language_code,
                category_name=row.category_name,
                story_title=row.story_title,
                source_row_numbers=(row.source_row_number,),
            )
        )
        return None
    if len(candidate_by_id) != 1:
        conflicts.append(
            MappingConflict(
                code="ambiguous_content_localization",
                message=(
                    f"Content title {row.story_title!r}/{row.language_code}/{row.canonical_type} "
                    f"matches content IDs {sorted(candidate_by_id)}"
                ),
                language_code=row.language_code,
                category_name=row.category_name,
                story_title=row.story_title,
                source_row_numbers=(row.source_row_number,),
            )
        )
        return None
    candidate = next(iter(candidate_by_id.values()))
    content = candidate["content"]
    localization = candidate["localization"]
    content_id = _required_id(content, "contentId")
    if content.get("active") is not True:
        conflicts.append(
            MappingConflict(
                code="inactive_content",
                message=f"Content {content_id} for {row.story_title!r} is inactive",
                language_code=row.language_code,
                category_name=row.category_name,
                story_title=row.story_title,
                source_row_numbers=(row.source_row_number,),
            )
        )
        return None
    if str(localization.get("status") or "").upper() != "PUBLISHED":
        conflicts.append(
            MappingConflict(
                code="unpublished_content_localization",
                message=(
                    f"Content {content_id} localization for {row.story_title!r}/"
                    f"{row.language_code} is not PUBLISHED"
                ),
                language_code=row.language_code,
                category_name=row.category_name,
                story_title=row.story_title,
                source_row_numbers=(row.source_row_number,),
            )
        )
        return None
    return content


def _verify_lane(lane: RemoteMappingLane, client: TellPalAdminClient) -> None:
    actual = client.list_category_contents(lane.category_id, lane.source.language_code)
    actual_pairs = sorted(
        (_required_id(item, "contentId"), _required_non_negative_int(item, "displayOrder"))
        for item in actual
    )
    expected_pairs = sorted((item.content_id, item.display_order) for item in lane.assignments)
    if actual_pairs != expected_pairs:
        raise CategoryStoryMappingImportError(
            f"Verification failed for {lane.source.category_name!r}/{lane.source.language_code}: "
            f"expected {expected_pairs}, received {actual_pairs}"
        )


def _required_id(payload: dict[str, object], field: str) -> int:
    value = payload.get(field)
    if isinstance(value, bool) or not isinstance(value, int) or value <= 0:
        raise CategoryStoryMappingImportError(f"Remote response is missing positive {field}")
    return value


def _required_non_negative_int(payload: dict[str, object], field: str) -> int:
    value = payload.get(field)
    if isinstance(value, bool) or not isinstance(value, int) or value < 0:
        raise CategoryStoryMappingImportError(f"Remote response has invalid non-negative {field}")
    return value


def utc_now() -> str:
    return datetime.now(UTC).isoformat()


__all__ = [
    "CategoryStoryMappingImportError",
    "MappingConflict",
    "RemoteMappingAssignment",
    "RemoteMappingLane",
    "RemoteMappingPlan",
    "execute_mapping_import",
    "format_preview",
    "remote_preflight",
    "require_ready",
]
