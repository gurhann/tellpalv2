import { useEffect, useRef } from "react";

import {
  shouldRefreshAssetPreviewUrl,
  hasUsableCachedDownloadUrl,
} from "@/features/assets/lib/asset-preview";
import { useRefreshAssetDownloadUrl } from "@/features/assets/mutations/use-refresh-asset-download-url";
import type { AssetViewModel } from "@/features/assets/model/asset-view-model";

type UseAssetPreviewResult = {
  previewUrl: string | null;
  previewStatus: "loading" | "available" | "unavailable" | "error";
  previewErrorMessage: string | null;
  isRefreshing: boolean;
  refreshPreview: (options?: { force?: boolean }) => Promise<void>;
};

export function useAssetPreview(
  asset: AssetViewModel | null,
  enabled: boolean,
): UseAssetPreviewResult {
  const {
    isPending: isRefreshing,
    problem,
    refreshAssetDownloadUrl,
  } = useRefreshAssetDownloadUrl();
  const lastAutoRefreshKeyRef = useRef<string | null>(null);
  const isPreviewable = enabled && asset?.isPreviewable === true;
  const previewUrl =
    asset && hasUsableCachedDownloadUrl(asset) ? asset.cachedDownloadUrl : null;

  useEffect(() => {
    if (!enabled) {
      lastAutoRefreshKeyRef.current = null;
      return;
    }

    if (!asset || !asset.isPreviewable) {
      return;
    }

    if (!shouldRefreshAssetPreviewUrl(asset) || isRefreshing) {
      return;
    }

    const autoRefreshKey = `${asset.id}:${asset.cachedDownloadUrl ?? "none"}:${asset.downloadUrlExpiresAt ?? "none"}`;

    if (lastAutoRefreshKeyRef.current === autoRefreshKey) {
      return;
    }

    lastAutoRefreshKeyRef.current = autoRefreshKey;
    void refreshAssetDownloadUrl(asset.id).catch(() => {
      return undefined;
    });
  }, [asset, enabled, isRefreshing, refreshAssetDownloadUrl]);

  async function refreshPreview(options: { force?: boolean } = {}) {
    if (!asset || !asset.isPreviewable) {
      return;
    }

    const shouldRefresh =
      options.force === true || shouldRefreshAssetPreviewUrl(asset);

    if (!shouldRefresh) {
      return;
    }

    lastAutoRefreshKeyRef.current = null;
    await refreshAssetDownloadUrl(asset.id);
  }

  if (!isPreviewable) {
    return {
      previewUrl: null,
      previewStatus: "unavailable",
      previewErrorMessage: null,
      isRefreshing: false,
      refreshPreview,
    };
  }

  if (previewUrl) {
    return {
      previewUrl,
      previewStatus: "available",
      previewErrorMessage: null,
      isRefreshing,
      refreshPreview,
    };
  }

  if (problem) {
    return {
      previewUrl: null,
      previewStatus: "error",
      previewErrorMessage: problem.detail ?? "Preview could not be loaded.",
      isRefreshing,
      refreshPreview,
    };
  }

  return {
    previewUrl: null,
    previewStatus: "loading",
    previewErrorMessage: null,
    isRefreshing,
    refreshPreview,
  };
}
