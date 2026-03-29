import type {
  AdminAssetResponse,
  AssetKind,
  AssetMediaType,
  AssetProvider,
} from "@/features/assets/api/asset-admin";
import type {
  AdminAssetProcessingResponse,
  AssetProcessingContentType,
  AssetProcessingState,
} from "@/features/assets/api/asset-processing-admin";
import { mapLanguage } from "@/lib/languages";

const assetProviderLabels: Record<AssetProvider, string> = {
  FIREBASE_STORAGE: "Firebase Storage",
  LOCAL_STUB: "Local Stub",
};

const assetKindLabels: Record<AssetKind, string> = {
  ORIGINAL_IMAGE: "Original Image",
  ORIGINAL_AUDIO: "Original Audio",
  THUMBNAIL_PHONE: "Phone Thumbnail",
  THUMBNAIL_TABLET: "Tablet Thumbnail",
  DETAIL_PHONE: "Phone Detail",
  DETAIL_TABLET: "Tablet Detail",
  OPTIMIZED_AUDIO: "Optimized Audio",
  CONTENT_ZIP: "Content ZIP",
  CONTENT_ZIP_PART1: "Content ZIP Part 1",
  CONTENT_ZIP_PART2: "Content ZIP Part 2",
};

const assetMediaTypeLabels: Record<AssetMediaType, string> = {
  IMAGE: "Image",
  AUDIO: "Audio",
  ARCHIVE: "Archive",
};

const processingContentTypeLabels: Record<AssetProcessingContentType, string> =
  {
    STORY: "Story",
    AUDIO_STORY: "Audio Story",
    MEDITATION: "Meditation",
    LULLABY: "Lullaby",
  };

const processingStateLabels: Record<AssetProcessingState, string> = {
  PENDING: "Pending",
  PROCESSING: "Processing",
  COMPLETED: "Completed",
  FAILED: "Failed",
};

export type AssetViewModel = {
  id: number;
  provider: AssetProvider;
  providerLabel: string;
  objectPath: string;
  mediaType: AssetMediaType;
  mediaTypeLabel: string;
  kind: AssetKind;
  kindLabel: string;
  mimeType: string | null;
  byteSize: number | null;
  checksumSha256: string | null;
  cachedDownloadUrl: string | null;
  downloadUrlCachedAt: string | null;
  downloadUrlExpiresAt: string | null;
  createdAt: string;
  updatedAt: string;
  hasCachedDownloadUrl: boolean;
  hasMetadata: boolean;
  hasChecksum: boolean;
};

export type AssetProcessingJobViewModel = {
  id: number;
  contentId: number;
  languageCode: string;
  languageLabel: string;
  contentType: AssetProcessingContentType;
  contentTypeLabel: string;
  externalKey: string;
  coverAssetId: number | null;
  audioAssetId: number | null;
  pageCount: number | null;
  status: AssetProcessingState;
  statusLabel: string;
  attemptCount: number;
  nextAttemptAt: string;
  leaseExpiresAt: string | null;
  startedAt: string | null;
  completedAt: string | null;
  failedAt: string | null;
  lastErrorCode: string | null;
  lastErrorMessage: string | null;
  createdAt: string;
  updatedAt: string;
  hasFailure: boolean;
  canRetry: boolean;
  isTerminal: boolean;
  isRunning: boolean;
};

export function mapAdminAsset(asset: AdminAssetResponse): AssetViewModel {
  return {
    id: asset.assetId,
    provider: asset.provider,
    providerLabel: assetProviderLabels[asset.provider],
    objectPath: asset.objectPath,
    mediaType: asset.mediaType,
    mediaTypeLabel: assetMediaTypeLabels[asset.mediaType],
    kind: asset.kind,
    kindLabel: assetKindLabels[asset.kind],
    mimeType: asset.mimeType,
    byteSize: asset.byteSize,
    checksumSha256: asset.checksumSha256,
    cachedDownloadUrl: asset.cachedDownloadUrl,
    downloadUrlCachedAt: asset.downloadUrlCachedAt,
    downloadUrlExpiresAt: asset.downloadUrlExpiresAt,
    createdAt: asset.createdAt,
    updatedAt: asset.updatedAt,
    hasCachedDownloadUrl: asset.cachedDownloadUrl !== null,
    hasMetadata: asset.mimeType !== null || asset.byteSize !== null,
    hasChecksum: asset.checksumSha256 !== null,
  };
}

export function mapAdminAssetList(
  assets: AdminAssetResponse[],
): AssetViewModel[] {
  return assets.map(mapAdminAsset);
}

export function mapAdminAssetProcessing(
  job: AdminAssetProcessingResponse,
): AssetProcessingJobViewModel {
  const language = mapLanguage(job.languageCode);
  const hasFailure = job.status === "FAILED";

  return {
    id: job.processingId,
    contentId: job.contentId,
    languageCode: language.code,
    languageLabel: language.label,
    contentType: job.contentType,
    contentTypeLabel: processingContentTypeLabels[job.contentType],
    externalKey: job.externalKey,
    coverAssetId: job.coverSourceAssetId,
    audioAssetId: job.audioSourceAssetId,
    pageCount: job.pageCount,
    status: job.status,
    statusLabel: processingStateLabels[job.status],
    attemptCount: job.attemptCount,
    nextAttemptAt: job.nextAttemptAt,
    leaseExpiresAt: job.leaseExpiresAt,
    startedAt: job.startedAt,
    completedAt: job.completedAt,
    failedAt: job.failedAt,
    lastErrorCode: job.lastErrorCode,
    lastErrorMessage: job.lastErrorMessage,
    createdAt: job.createdAt,
    updatedAt: job.updatedAt,
    hasFailure,
    canRetry: hasFailure,
    isTerminal: job.status === "COMPLETED" || hasFailure,
    isRunning: job.status === "PROCESSING",
  };
}

export function mapAdminAssetProcessingList(
  jobs: AdminAssetProcessingResponse[],
): AssetProcessingJobViewModel[] {
  return jobs.map(mapAdminAssetProcessing);
}
