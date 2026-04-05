import type { AdminAssetResponse } from "@/features/assets/api/asset-admin";
import type { AdminAssetProcessingResponse } from "@/features/assets/api/asset-processing-admin";
import {
  mapAdminAsset,
  mapAdminAssetProcessing,
} from "@/features/assets/model/asset-view-model";

describe("asset view model mappers", () => {
  it("maps asset metadata with derived labels", () => {
    const dto: AdminAssetResponse = {
      assetId: 5,
      provider: "FIREBASE_STORAGE",
      objectPath: "/content/story/en/original/cover.png",
      mediaType: "IMAGE",
      kind: "ORIGINAL_IMAGE",
      mimeType: "image/png",
      byteSize: 2048,
      checksumSha256: "abc123",
      cachedDownloadUrl: "https://example.com/download",
      downloadUrlCachedAt: "2026-03-29T12:00:00Z",
      downloadUrlExpiresAt: "2026-06-29T13:00:00Z",
      createdAt: "2026-03-28T12:00:00Z",
      updatedAt: "2026-03-29T12:00:00Z",
    };

    expect(mapAdminAsset(dto)).toEqual({
      id: 5,
      provider: "FIREBASE_STORAGE",
      providerLabel: "Firebase Storage",
      objectPath: "/content/story/en/original/cover.png",
      mediaType: "IMAGE",
      mediaTypeLabel: "Image",
      kind: "ORIGINAL_IMAGE",
      kindLabel: "Original Image",
      mimeType: "image/png",
      byteSize: 2048,
      checksumSha256: "abc123",
      cachedDownloadUrl: "https://example.com/download",
      downloadUrlCachedAt: "2026-03-29T12:00:00Z",
      downloadUrlExpiresAt: "2026-06-29T13:00:00Z",
      createdAt: "2026-03-28T12:00:00Z",
      updatedAt: "2026-03-29T12:00:00Z",
      hasCachedDownloadUrl: true,
      hasUsableCachedDownloadUrl: true,
      hasMetadata: true,
      hasChecksum: true,
      isPreviewable: true,
      previewKind: "image",
    });
  });

  it("maps processing jobs with retry and failure flags", () => {
    const dto: AdminAssetProcessingResponse = {
      processingId: 91,
      contentId: 14,
      languageCode: "TR",
      contentType: "STORY",
      externalKey: "sleep-story",
      coverSourceAssetId: 5,
      audioSourceAssetId: null,
      pageCount: 8,
      status: "FAILED",
      attemptCount: 3,
      nextAttemptAt: "2026-03-29T14:00:00Z",
      leaseExpiresAt: null,
      startedAt: "2026-03-29T13:00:00Z",
      completedAt: null,
      failedAt: "2026-03-29T13:05:00Z",
      lastErrorCode: "ffmpeg_failed",
      lastErrorMessage: "Audio pipeline timed out.",
      createdAt: "2026-03-29T12:30:00Z",
      updatedAt: "2026-03-29T13:05:00Z",
    };

    expect(mapAdminAssetProcessing(dto)).toEqual({
      id: 91,
      contentId: 14,
      languageCode: "tr",
      languageLabel: "Turkish",
      contentType: "STORY",
      contentTypeLabel: "Story",
      externalKey: "sleep-story",
      coverAssetId: 5,
      audioAssetId: null,
      pageCount: 8,
      status: "FAILED",
      statusLabel: "Failed",
      attemptCount: 3,
      nextAttemptAt: "2026-03-29T14:00:00Z",
      leaseExpiresAt: null,
      startedAt: "2026-03-29T13:00:00Z",
      completedAt: null,
      failedAt: "2026-03-29T13:05:00Z",
      lastErrorCode: "ffmpeg_failed",
      lastErrorMessage: "Audio pipeline timed out.",
      createdAt: "2026-03-29T12:30:00Z",
      updatedAt: "2026-03-29T13:05:00Z",
      hasFailure: true,
      canRetry: true,
      isTerminal: true,
      isRunning: false,
    });
  });
});
