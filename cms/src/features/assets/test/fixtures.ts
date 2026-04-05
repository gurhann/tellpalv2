import type { AdminAssetResponse } from "@/features/assets/api/asset-admin";
import {
  mapAdminAsset,
  type AssetViewModel,
} from "@/features/assets/model/asset-view-model";

export const phoneThumbnailAssetResponse: AdminAssetResponse = {
  assetId: 4,
  provider: "LOCAL_STUB",
  objectPath: "/content/images/evening-garden-page-1.jpg",
  mediaType: "IMAGE",
  kind: "THUMBNAIL_PHONE",
  mimeType: "image/jpeg",
  byteSize: 98_112,
  checksumSha256: "image-checksum-4",
  cachedDownloadUrl: "https://cdn.tellpal.test/assets/4",
  downloadUrlCachedAt: "2026-03-31T11:00:00Z",
  downloadUrlExpiresAt: "2026-03-31T12:00:00Z",
  createdAt: "2026-03-31T10:45:00Z",
  updatedAt: "2026-03-31T11:00:00Z",
};

export const originalAudioAssetResponse: AdminAssetResponse = {
  assetId: 1,
  provider: "LOCAL_STUB",
  objectPath: "/content/audio/rain-room-en.wav",
  mediaType: "AUDIO",
  kind: "ORIGINAL_AUDIO",
  mimeType: "audio/wav",
  byteSize: 5_242_880,
  checksumSha256: "audio-checksum-1",
  cachedDownloadUrl: null,
  downloadUrlCachedAt: null,
  downloadUrlExpiresAt: null,
  createdAt: "2026-03-30T09:00:00Z",
  updatedAt: "2026-03-30T09:00:00Z",
};

export const contentArchiveAssetResponse: AdminAssetResponse = {
  assetId: 9,
  provider: "FIREBASE_STORAGE",
  objectPath: "/content/packages/story.evening-garden.en.zip",
  mediaType: "ARCHIVE",
  kind: "CONTENT_ZIP",
  mimeType: "application/zip",
  byteSize: 2_129_920,
  checksumSha256: null,
  cachedDownloadUrl: "https://cdn.tellpal.test/assets/9",
  downloadUrlCachedAt: "2026-03-31T11:30:00Z",
  downloadUrlExpiresAt: "2026-03-31T13:30:00Z",
  createdAt: "2026-03-31T11:25:00Z",
  updatedAt: "2026-03-31T11:30:00Z",
};

export const uploadedFirebaseAudioAssetResponse: AdminAssetResponse = {
  assetId: 11,
  provider: "FIREBASE_STORAGE",
  objectPath:
    "/local/manual/audio/original/2026/04/asset-11-bedtime-breeze.mp3",
  mediaType: "AUDIO",
  kind: "ORIGINAL_AUDIO",
  mimeType: "audio/mpeg",
  byteSize: 8_192,
  checksumSha256: null,
  cachedDownloadUrl: null,
  downloadUrlCachedAt: null,
  downloadUrlExpiresAt: null,
  createdAt: "2026-04-04T18:20:00Z",
  updatedAt: "2026-04-04T18:20:00Z",
};

export const uploadedFirebaseImageAssetResponse: AdminAssetResponse = {
  assetId: 12,
  provider: "FIREBASE_STORAGE",
  objectPath:
    "/local/manual/images/original/2026/04/asset-12-bedtime-cover.jpg",
  mediaType: "IMAGE",
  kind: "ORIGINAL_IMAGE",
  mimeType: "image/jpeg",
  byteSize: 4_096,
  checksumSha256: null,
  cachedDownloadUrl: null,
  downloadUrlCachedAt: null,
  downloadUrlExpiresAt: null,
  createdAt: "2026-04-04T18:25:00Z",
  updatedAt: "2026-04-04T18:25:00Z",
};

export const assetResponses: AdminAssetResponse[] = [
  phoneThumbnailAssetResponse,
  originalAudioAssetResponse,
  contentArchiveAssetResponse,
];

export const phoneThumbnailAssetViewModel = mapAdminAsset(
  phoneThumbnailAssetResponse,
);
export const originalAudioAssetViewModel = mapAdminAsset(
  originalAudioAssetResponse,
);
export const contentArchiveAssetViewModel = mapAdminAsset(
  contentArchiveAssetResponse,
);
export const uploadedFirebaseAudioAssetViewModel = mapAdminAsset(
  uploadedFirebaseAudioAssetResponse,
);

export const assetViewModels: AssetViewModel[] = [
  phoneThumbnailAssetViewModel,
  originalAudioAssetViewModel,
  contentArchiveAssetViewModel,
];
