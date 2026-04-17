import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import type { PropsWithChildren } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import type { AdminAssetResponse } from "@/features/assets/api/asset-admin";
import {
  contentArchiveAssetViewModel,
  originalAudioAssetViewModel,
  phoneThumbnailAssetResponse,
} from "@/features/assets/test/fixtures";
import { mapAdminAsset } from "@/features/assets/model/asset-view-model";
import { queryKeys } from "@/lib/query-keys";

import { useAssetPreview } from "./use-asset-preview";

const assetAdminApiMock = vi.hoisted(() => ({
  refreshDownloadUrlCache: vi.fn(),
}));

vi.mock("@/features/assets/api/asset-admin", async () => {
  const actual = await vi.importActual<
    typeof import("@/features/assets/api/asset-admin")
  >("@/features/assets/api/asset-admin");

  return {
    ...actual,
    assetAdminApi: {
      ...actual.assetAdminApi,
      refreshDownloadUrlCache: assetAdminApiMock.refreshDownloadUrlCache,
    },
  };
});

function createWrapper(queryClient: QueryClient) {
  return function Wrapper({ children }: PropsWithChildren) {
    return (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );
  };
}

beforeEach(() => {
  assetAdminApiMock.refreshDownloadUrlCache.mockReset();
});

describe("useAssetPreview", () => {
  it("auto-refreshes previewable assets whose cached URL is missing", async () => {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
        mutations: {
          retry: false,
        },
      },
    });
    const refreshedAsset: AdminAssetResponse = {
      ...phoneThumbnailAssetResponse,
      assetId: 1,
      objectPath:
        "/local/manual/audio/original/2026/04/asset-1-rain-room-en.wav",
      mediaType: "AUDIO",
      kind: "ORIGINAL_AUDIO",
      mimeType: "audio/wav",
      cachedDownloadUrl:
        "https://storage.googleapis.com/tellpal/local/audio-1.wav",
      downloadUrlCachedAt: "2026-05-05T12:00:00Z",
      downloadUrlExpiresAt: "2026-05-05T13:00:00Z",
      updatedAt: "2026-05-05T12:00:00Z",
    };

    queryClient.setQueryData(
      queryKeys.assets.detail(1),
      originalAudioAssetViewModel,
    );
    assetAdminApiMock.refreshDownloadUrlCache.mockResolvedValue(refreshedAsset);

    const { result, rerender } = renderHook(
      ({ asset }) => useAssetPreview(asset, true),
      {
        initialProps: {
          asset: originalAudioAssetViewModel,
        },
        wrapper: createWrapper(queryClient),
      },
    );

    await waitFor(() => {
      expect(assetAdminApiMock.refreshDownloadUrlCache).toHaveBeenCalledWith(1);
    });

    await waitFor(() => {
      expect(
        (
          queryClient.getQueryData(
            queryKeys.assets.detail(1),
          ) as ReturnType<typeof mapAdminAsset> | undefined
        )?.cachedDownloadUrl,
      ).toBe("https://storage.googleapis.com/tellpal/local/audio-1.wav");
    });

    const cachedAsset = queryClient.getQueryData(
      queryKeys.assets.detail(1),
    ) as ReturnType<typeof mapAdminAsset>;

    rerender({ asset: cachedAsset });

    await waitFor(() => {
      expect(result.current.previewStatus).toBe("available");
      expect(result.current.previewUrl).toBe(
        "https://storage.googleapis.com/tellpal/local/audio-1.wav",
      );
    });
  });

  it("keeps archive assets in unavailable state without refreshing", async () => {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
        mutations: {
          retry: false,
        },
      },
    });

    const { result } = renderHook(
      () => useAssetPreview(contentArchiveAssetViewModel, true),
      {
        wrapper: createWrapper(queryClient),
      },
    );

    await waitFor(() => {
      expect(result.current.previewStatus).toBe("unavailable");
    });

    expect(assetAdminApiMock.refreshDownloadUrlCache).not.toHaveBeenCalled();
  });

  it("surfaces refresh failures as preview errors", async () => {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
        mutations: {
          retry: false,
        },
      },
    });

    assetAdminApiMock.refreshDownloadUrlCache.mockRejectedValue(
      new Error("Signed URL refresh failed"),
    );

    const { result } = renderHook(
      () => useAssetPreview(originalAudioAssetViewModel, true),
      {
        wrapper: createWrapper(queryClient),
      },
    );

    await waitFor(() => {
      expect(result.current.previewStatus).toBe("error");
    });

    expect(result.current.previewErrorMessage).toMatch(
      /signed url refresh failed/i,
    );
  });
});
