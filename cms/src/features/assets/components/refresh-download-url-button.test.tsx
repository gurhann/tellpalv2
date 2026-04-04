import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import type { PropsWithChildren } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import type { AdminAssetResponse } from "@/features/assets/api/asset-admin";
import { phoneThumbnailAssetViewModel } from "@/features/assets/test/fixtures";
import { queryKeys } from "@/lib/query-keys";

import { RefreshDownloadUrlButton } from "./refresh-download-url-button";

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

describe("RefreshDownloadUrlButton", () => {
  it("refreshes the cached download URL and updates detail cache", async () => {
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
      assetId: 4,
      provider: "LOCAL_STUB",
      objectPath: "/content/images/evening-garden-page-1.jpg",
      mediaType: "IMAGE",
      kind: "THUMBNAIL_PHONE",
      mimeType: "image/jpeg",
      byteSize: 98_112,
      checksumSha256: "image-checksum-4",
      cachedDownloadUrl: "https://cdn.tellpal.test/assets/4?fresh=true",
      downloadUrlCachedAt: "2026-04-04T16:00:00Z",
      downloadUrlExpiresAt: "2026-04-04T17:00:00Z",
      createdAt: "2026-03-31T10:45:00Z",
      updatedAt: "2026-04-04T16:00:00Z",
    };

    queryClient.setQueryData(
      queryKeys.assets.detail(4),
      phoneThumbnailAssetViewModel,
    );
    assetAdminApiMock.refreshDownloadUrlCache.mockResolvedValue(refreshedAsset);

    render(<RefreshDownloadUrlButton asset={phoneThumbnailAssetViewModel} />, {
      wrapper: createWrapper(queryClient),
    });

    fireEvent.click(
      screen.getByRole("button", { name: /refresh cached url/i }),
    );

    await waitFor(() => {
      expect(assetAdminApiMock.refreshDownloadUrlCache).toHaveBeenCalledWith(4);
    });

    expect(
      queryClient.getQueryData<{ cachedDownloadUrl: string | null }>(
        queryKeys.assets.detail(4),
      ),
    ).toMatchObject({
      cachedDownloadUrl: "https://cdn.tellpal.test/assets/4?fresh=true",
    });
  });
});
