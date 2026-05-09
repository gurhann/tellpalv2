import { useEffect, useRef, useState } from "react";
import { useMutation } from "@tanstack/react-query";

import { assetAdminApi } from "@/features/assets/api/asset-admin";
import {
  ASSET_PREVIEW_EXPIRY_BUFFER_MS,
  normalizeBackendPreviewUrl,
} from "@/features/assets/lib/asset-preview";
import type { AssetViewModel } from "@/features/assets/model/asset-view-model";
import { appEnv } from "@/lib/env";
import { ApiClientError } from "@/lib/http/client";

type UseAssetPreviewResult = {
  previewUrl: string | null;
  previewStatus: "loading" | "available" | "unavailable" | "error";
  previewErrorMessage: string | null;
  isRefreshing: boolean;
  refreshPreview: (options?: { force?: boolean }) => Promise<void>;
};

type PreviewTokenState = {
  assetId: number;
  previewUrl: string;
  expiresAt: string;
  objectUrl: boolean;
};

function hasUsablePreviewToken(
  tokenState: PreviewTokenState | null,
  assetId: number,
  now = Date.now(),
) {
  if (!tokenState || tokenState.assetId !== assetId) {
    return false;
  }
  const expiresAt = Date.parse(tokenState.expiresAt);
  if (Number.isNaN(expiresAt)) {
    return false;
  }
  return expiresAt - now > ASSET_PREVIEW_EXPIRY_BUFFER_MS;
}

function revokePreviewUrl(tokenState: PreviewTokenState | null) {
  if (tokenState?.objectUrl) {
    URL.revokeObjectURL(tokenState.previewUrl);
  }
}

function canCreateObjectUrl() {
  return (
    typeof URL !== "undefined" &&
    typeof URL.createObjectURL === "function" &&
    typeof URL.revokeObjectURL === "function"
  );
}

async function createAudioObjectUrl(previewUrl: string, mimeType: string | null) {
  if (!canCreateObjectUrl()) {
    return { previewUrl, objectUrl: false };
  }

  try {
    const response = await fetch(previewUrl, { cache: "no-store" });
    if (!response.ok) {
      return { previewUrl, objectUrl: false };
    }

    const sourceBlob = await response.blob();
    const blob =
      mimeType && sourceBlob.type !== mimeType
        ? sourceBlob.slice(0, sourceBlob.size, mimeType)
        : sourceBlob;

    return { previewUrl: URL.createObjectURL(blob), objectUrl: true };
  } catch {
    return { previewUrl, objectUrl: false };
  }
}

function previewErrorMessage(error: unknown) {
  if (!error) {
    return null;
  }
  if (error instanceof ApiClientError) {
    return error.problem.detail ?? "Preview could not be loaded.";
  }
  return error instanceof Error
    ? error.message
    : "Preview could not be loaded.";
}

export function useAssetPreview(
  asset: AssetViewModel | null,
  enabled: boolean,
): UseAssetPreviewResult {
  const [previewToken, setPreviewToken] = useState<PreviewTokenState | null>(
    null,
  );
  const contentTokenMutation = useMutation({
    mutationFn: async (targetAsset: AssetViewModel) => {
      const response = await assetAdminApi.issueAssetContentToken(
        targetAsset.id,
      );
      const normalizedPreviewUrl = normalizeBackendPreviewUrl(
        response.previewUrl,
        appEnv.VITE_API_BASE_URL,
      );
      const previewSource =
        targetAsset.previewKind === "audio"
          ? await createAudioObjectUrl(normalizedPreviewUrl, targetAsset.mimeType)
          : { previewUrl: normalizedPreviewUrl, objectUrl: false };

      return {
        assetId: targetAsset.id,
        previewUrl: previewSource.previewUrl,
        expiresAt: response.expiresAt,
        objectUrl: previewSource.objectUrl,
      };
    },
    onSuccess: (nextToken) => {
      setPreviewToken((currentToken) => {
        revokePreviewUrl(currentToken);
        return nextToken;
      });
    },
  });
  const issueContentToken = contentTokenMutation.mutateAsync;
  const isRefreshing = contentTokenMutation.isPending;
  const refreshError = contentTokenMutation.error;
  const lastAutoRefreshKeyRef = useRef<string | null>(null);
  const isPreviewable = enabled && asset?.isPreviewable === true;
  const previewUrl =
    asset && hasUsablePreviewToken(previewToken, asset.id)
      ? (previewToken?.previewUrl ?? null)
      : null;

  useEffect(() => {
    if (!enabled) {
      lastAutoRefreshKeyRef.current = null;
      return;
    }

    if (!asset || !asset.isPreviewable) {
      return;
    }

    if (hasUsablePreviewToken(previewToken, asset.id)) {
      return;
    }

    if (isRefreshing) {
      return;
    }

    const autoRefreshKey = `${asset.id}:${asset.updatedAt}`;

    if (lastAutoRefreshKeyRef.current === autoRefreshKey) {
      return;
    }

    lastAutoRefreshKeyRef.current = autoRefreshKey;
    void issueContentToken(asset).catch(() => {
      return undefined;
    });
  }, [asset, enabled, isRefreshing, issueContentToken, previewToken]);

  useEffect(() => {
    return () => {
      revokePreviewUrl(previewToken);
    };
  }, [previewToken]);

  async function refreshPreview(options: { force?: boolean } = {}) {
    if (!asset || !asset.isPreviewable) {
      return;
    }

    const shouldRefresh =
      options.force === true || !hasUsablePreviewToken(previewToken, asset.id);

    if (!shouldRefresh) {
      return;
    }

    lastAutoRefreshKeyRef.current = null;
    await issueContentToken(asset);
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

  const errorMessage = previewErrorMessage(refreshError);
  if (errorMessage) {
    return {
      previewUrl: null,
      previewStatus: "error",
      previewErrorMessage: errorMessage,
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
