import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook } from "@testing-library/react";
import type { PropsWithChildren } from "react";
import { act } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import type { AdminAssetResponse } from "@/features/assets/api/asset-admin";
import { phoneThumbnailAssetViewModel } from "@/features/assets/test/fixtures";
import { queryKeys } from "@/lib/query-keys";

import { useUpdateAssetMetadata } from "./use-update-asset-metadata";

const assetAdminApiMock = vi.hoisted(() => ({
  updateAssetMetadata: vi.fn(),
}));

vi.mock("@/features/assets/api/asset-admin", async () => {
  const actual = await vi.importActual<
    typeof import("@/features/assets/api/asset-admin")
  >("@/features/assets/api/asset-admin");

  return {
    ...actual,
    assetAdminApi: {
      ...actual.assetAdminApi,
      updateAssetMetadata: assetAdminApiMock.updateAssetMetadata,
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
  assetAdminApiMock.updateAssetMetadata.mockReset();
});

describe("useUpdateAssetMetadata", () => {
  it("updates recent/detail caches after a metadata save", async () => {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
      },
    });
    const updatedAsset: AdminAssetResponse = {
      assetId: 4,
      provider: "LOCAL_STUB",
      objectPath: "/content/images/evening-garden-page-1.jpg",
      mediaType: "IMAGE",
      kind: "THUMBNAIL_PHONE",
      mimeType: "image/webp",
      byteSize: 100_000,
      checksumSha256: "updated-checksum",
      cachedDownloadUrl: "https://cdn.tellpal.test/assets/4",
      downloadUrlCachedAt: "2026-03-31T11:00:00Z",
      downloadUrlExpiresAt: "2026-03-31T12:00:00Z",
      createdAt: "2026-03-31T10:45:00Z",
      updatedAt: "2026-03-31T11:30:00Z",
    };
    const invalidateQueries = vi.spyOn(queryClient, "invalidateQueries");

    queryClient.setQueryData(queryKeys.assets.recent({ limit: 24 }), [
      phoneThumbnailAssetViewModel,
    ]);
    queryClient.setQueryData(
      queryKeys.assets.detail(4),
      phoneThumbnailAssetViewModel,
    );
    assetAdminApiMock.updateAssetMetadata.mockResolvedValue(updatedAsset);

    const { result } = renderHook(() => useUpdateAssetMetadata(4), {
      wrapper: createWrapper(queryClient),
    });

    await act(async () => {
      await result.current.mutateAsync({
        mimeType: "image/webp",
        byteSize: 100000,
        checksumSha256: "updated-checksum",
      });
    });

    expect(assetAdminApiMock.updateAssetMetadata).toHaveBeenCalledWith(4, {
      mimeType: "image/webp",
      byteSize: 100000,
      checksumSha256: "updated-checksum",
    });
    expect(
      queryClient.getQueryData<{ mimeType: string | null }>(
        queryKeys.assets.detail(4),
      ),
    ).toMatchObject({
      mimeType: "image/webp",
    });
    expect(
      queryClient.getQueryData<Array<{ mimeType: string | null }>>(
        queryKeys.assets.recent({ limit: 24 }),
      ),
    ).toEqual(
      expect.arrayContaining([
        expect.objectContaining({ mimeType: "image/webp" }),
      ]),
    );
    expect(invalidateQueries).toHaveBeenCalledWith({
      queryKey: ["assets", "recent"],
    });
    expect(invalidateQueries).toHaveBeenCalledWith({
      queryKey: queryKeys.assets.detail(4),
    });
  });
});
