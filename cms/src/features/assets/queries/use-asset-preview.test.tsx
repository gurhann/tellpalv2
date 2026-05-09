import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import type { PropsWithChildren } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import {
  contentArchiveAssetViewModel,
  originalAudioAssetViewModel,
} from "@/features/assets/test/fixtures";

import { useAssetPreview } from "./use-asset-preview";

const assetAdminApiMock = vi.hoisted(() => ({
  issueAssetContentToken: vi.fn(),
}));

vi.mock("@/features/assets/api/asset-admin", async () => {
  const actual = await vi.importActual<
    typeof import("@/features/assets/api/asset-admin")
  >("@/features/assets/api/asset-admin");

  return {
    ...actual,
    assetAdminApi: {
      ...actual.assetAdminApi,
      issueAssetContentToken: assetAdminApiMock.issueAssetContentToken,
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
  assetAdminApiMock.issueAssetContentToken.mockReset();
});

describe("useAssetPreview", () => {
  it("loads a backend preview URL for previewable assets", async () => {
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
    assetAdminApiMock.issueAssetContentToken.mockResolvedValue({
      previewUrl:
        "https://api.tellpal.test/api/admin/media/1/content?token=preview-token",
      expiresAt: new Date(Date.now() + 60 * 60 * 1000).toISOString(),
    });

    const { result } = renderHook(
      () => useAssetPreview(originalAudioAssetViewModel, true),
      {
        wrapper: createWrapper(queryClient),
      },
    );

    await waitFor(() => {
      expect(assetAdminApiMock.issueAssetContentToken).toHaveBeenCalledWith(1);
    });

    await waitFor(() => expect(result.current.previewStatus).toBe("available"));
    expect(result.current.previewUrl).toBe(
      "https://api.tellpal.test/api/admin/media/1/content?token=preview-token",
    );
    expect(result.current.previewUrl).not.toContain("storage.googleapis.com");
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

    expect(assetAdminApiMock.issueAssetContentToken).not.toHaveBeenCalled();
  });

  it("surfaces backend preview token failures as preview errors", async () => {
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

    assetAdminApiMock.issueAssetContentToken.mockRejectedValue(
      new Error("Preview token failed"),
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

    expect(result.current.previewErrorMessage).toMatch(/preview token failed/i);
  });

  it("refreshes the backend preview URL on demand", async () => {
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

    assetAdminApiMock.issueAssetContentToken
      .mockResolvedValueOnce({
        previewUrl:
          "https://api.tellpal.test/api/admin/media/1/content?token=first-token",
        expiresAt: new Date(Date.now() + 60 * 60 * 1000).toISOString(),
      })
      .mockResolvedValueOnce({
        previewUrl:
          "https://api.tellpal.test/api/admin/media/1/content?token=second-token",
        expiresAt: new Date(Date.now() + 70 * 60 * 1000).toISOString(),
      });

    const { result } = renderHook(
      () => useAssetPreview(originalAudioAssetViewModel, true),
      {
        wrapper: createWrapper(queryClient),
      },
    );

    await waitFor(() => expect(result.current.previewStatus).toBe("available"));

    await result.current.refreshPreview({ force: true });

    await waitFor(() => {
      expect(result.current.previewUrl).toBe(
        "https://api.tellpal.test/api/admin/media/1/content?token=second-token",
      );
    });
  });
});
