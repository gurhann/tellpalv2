import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook } from "@testing-library/react";
import type { PropsWithChildren } from "react";
import { act } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import {
  failedProcessingResponse,
  processingResponse,
} from "@/features/assets/test/fixtures";
import { queryKeys } from "@/lib/query-keys";

import { useProcessingActions } from "./use-processing-actions";

const assetProcessingAdminApiMock = vi.hoisted(() => ({
  scheduleProcessing: vi.fn(),
  retryProcessing: vi.fn(),
}));

vi.mock("@/features/assets/api/asset-processing-admin", async () => {
  const actual = await vi.importActual<
    typeof import("@/features/assets/api/asset-processing-admin")
  >("@/features/assets/api/asset-processing-admin");

  return {
    ...actual,
    assetProcessingAdminApi: {
      ...actual.assetProcessingAdminApi,
      scheduleProcessing: assetProcessingAdminApiMock.scheduleProcessing,
      retryProcessing: assetProcessingAdminApiMock.retryProcessing,
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
  assetProcessingAdminApiMock.scheduleProcessing.mockReset();
  assetProcessingAdminApiMock.retryProcessing.mockReset();
});

describe("useProcessingActions", () => {
  it("schedules processing and invalidates recent/status caches", async () => {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
      },
    });
    const invalidateQueries = vi.spyOn(queryClient, "invalidateQueries");

    assetProcessingAdminApiMock.scheduleProcessing.mockResolvedValue(
      processingResponse,
    );

    const { result } = renderHook(() => useProcessingActions(), {
      wrapper: createWrapper(queryClient),
    });

    await act(async () => {
      await result.current.scheduleProcessing.mutateAsync({
        contentId: 2,
        languageCode: "de",
        contentType: "MEDITATION",
        externalKey: "meditation.rain-room",
        coverSourceAssetId: 12,
        audioSourceAssetId: 11,
      });
    });

    expect(assetProcessingAdminApiMock.scheduleProcessing).toHaveBeenCalledWith({
      contentId: 2,
      languageCode: "de",
      contentType: "MEDITATION",
      externalKey: "meditation.rain-room",
      coverSourceAssetId: 12,
      audioSourceAssetId: 11,
    });
    expect(invalidateQueries).toHaveBeenCalledWith({
      queryKey: queryKeys.assets.processingRecentRoot(),
    });
    expect(invalidateQueries).toHaveBeenCalledWith({
      queryKey: queryKeys.assets.processingStatus(2, "de"),
    });
  });

  it("retries a failed job and invalidates recent/status caches", async () => {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
      },
    });
    const invalidateQueries = vi.spyOn(queryClient, "invalidateQueries");

    assetProcessingAdminApiMock.retryProcessing.mockResolvedValue(
      failedProcessingResponse,
    );

    const { result } = renderHook(() => useProcessingActions(), {
      wrapper: createWrapper(queryClient),
    });

    await act(async () => {
      await result.current.retryProcessing.mutateAsync({
        contentId: 1,
        languageCode: "en",
        input: {
          contentType: "STORY",
          externalKey: "story.evening-garden",
          coverSourceAssetId: 12,
          audioSourceAssetId: null,
          pageCount: 2,
        },
      });
    });

    expect(assetProcessingAdminApiMock.retryProcessing).toHaveBeenCalledWith(
      1,
      "en",
      {
        contentType: "STORY",
        externalKey: "story.evening-garden",
        coverSourceAssetId: 12,
        audioSourceAssetId: null,
        pageCount: 2,
      },
    );
    expect(invalidateQueries).toHaveBeenCalledWith({
      queryKey: queryKeys.assets.processingRecentRoot(),
    });
    expect(invalidateQueries).toHaveBeenCalledWith({
      queryKey: queryKeys.assets.processingStatus(1, "en"),
    });
  });
});
