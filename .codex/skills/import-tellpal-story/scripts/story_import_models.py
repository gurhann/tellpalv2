from __future__ import annotations

from dataclasses import dataclass, field


@dataclass(frozen=True)
class ContributorResolution:
    display_name: str
    contributor_id: int | None


@dataclass(frozen=True)
class UploadedAsset:
    asset_id: int
    relative_path: str
    kind: str
    checksum_sha256: str
    byte_size: int


@dataclass
class ImportState:
    content_id: int | None = None
    localized_cover_ids: dict[str, int] = field(default_factory=dict)
    page_media_ids: dict[tuple[str, int], tuple[int, int]] = field(default_factory=dict)
    textless_cover_id: int | None = None
    textless_page_ids: dict[int, int] = field(default_factory=dict)
    uploaded_assets: dict[int, UploadedAsset] = field(default_factory=dict)
    contributor_ids: dict[str, int] = field(default_factory=dict)
    contributor_assignments: list[dict[str, object]] = field(default_factory=list)
