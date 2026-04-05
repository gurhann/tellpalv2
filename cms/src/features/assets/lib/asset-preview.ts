import type { AssetMediaType } from "@/features/assets/api/asset-admin";

type PreviewAssetLike = {
  mediaType: AssetMediaType;
  cachedDownloadUrl: string | null;
  downloadUrlExpiresAt: string | null;
};

export type AssetPreviewKind = "image" | "audio" | "none";

export const ASSET_PREVIEW_EXPIRY_BUFFER_MS = 5 * 60 * 1000;

export function getAssetPreviewKind(
  mediaType: AssetMediaType,
): AssetPreviewKind {
  if (mediaType === "IMAGE") {
    return "image";
  }

  if (mediaType === "AUDIO") {
    return "audio";
  }

  return "none";
}

export function isPreviewableAsset(mediaType: AssetMediaType) {
  return getAssetPreviewKind(mediaType) !== "none";
}

export function hasUsableCachedDownloadUrl(
  asset: PreviewAssetLike,
  now = Date.now(),
) {
  if (!asset.cachedDownloadUrl) {
    return false;
  }

  if (!asset.downloadUrlExpiresAt) {
    return true;
  }

  const expiresAt = Date.parse(asset.downloadUrlExpiresAt);

  if (Number.isNaN(expiresAt)) {
    return true;
  }

  return expiresAt - now > ASSET_PREVIEW_EXPIRY_BUFFER_MS;
}

export function shouldRefreshAssetPreviewUrl(
  asset: PreviewAssetLike,
  now = Date.now(),
) {
  return (
    isPreviewableAsset(asset.mediaType) &&
    !hasUsableCachedDownloadUrl(asset, now)
  );
}
