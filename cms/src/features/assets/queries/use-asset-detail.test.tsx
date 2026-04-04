import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import type { PropsWithChildren } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { phoneThumbnailAssetResponse } from "@/features/assets/test/fixtures";
import { ApiClientError } from "@/lib/http/client";
import type { ApiProblemDetail } from "@/types/api";

import { useAssetDetail } from "./use-asset-detail";

const assetAdminApiMock = vi.hoisted(() => ({
  getAsset: vi.fn(),
}));

vi.mock("@/features/assets/api/asset-admin", async () => {
  const actual = await vi.importActual<
    typeof import("@/features/assets/api/asset-admin")
  >("@/features/assets/api/asset-admin");

  return {
    ...actual,
    assetAdminApi: {
      ...actual.assetAdminApi,
      getAsset: assetAdminApiMock.getAsset,
    },
  };
});

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  });

  return function Wrapper({ children }: PropsWithChildren) {
    return (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );
  };
}

function makeProblem(
  overrides: Partial<ApiProblemDetail> = {},
): ApiProblemDetail {
  return {
    type: "about:blank",
    title: "Request failed",
    status: 404,
    detail: "Asset record missing",
    ...overrides,
  };
}

function makeApiClientError(problem: ApiProblemDetail) {
  return new ApiClientError(
    problem,
    new Response(JSON.stringify(problem), {
      status: problem.status,
      statusText: problem.title,
      headers: {
        "Content-Type": "application/problem+json",
      },
    }),
  );
}

beforeEach(() => {
  assetAdminApiMock.getAsset.mockReset();
});

describe("useAssetDetail", () => {
  it("loads and maps one asset detail record", async () => {
    assetAdminApiMock.getAsset.mockResolvedValue(phoneThumbnailAssetResponse);

    const { result } = renderHook(() => useAssetDetail(4), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(assetAdminApiMock.getAsset).toHaveBeenCalledWith(4);
    expect(result.current.asset).toMatchObject({
      id: 4,
      objectPath: "/content/images/evening-garden-page-1.jpg",
      kindLabel: "Phone Thumbnail",
    });
    expect(result.current.problem).toBeNull();
    expect(result.current.isNotFound).toBe(false);
  });

  it("marks detail queries as not found for asset_not_found responses", async () => {
    assetAdminApiMock.getAsset.mockRejectedValue(
      makeApiClientError(
        makeProblem({
          title: "Asset not found",
          detail: "Asset 404 was not found",
          errorCode: "asset_not_found",
          path: "/api/admin/media/404",
        }),
      ),
    );

    const { result } = renderHook(() => useAssetDetail(404), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });

    expect(result.current.isNotFound).toBe(true);
    expect(result.current.problem?.status).toBe(404);
  });

  it("keeps generic detail failures separate from not-found handling", async () => {
    assetAdminApiMock.getAsset.mockRejectedValue(
      new Error("Asset lookup timeout"),
    );

    const { result } = renderHook(() => useAssetDetail(4), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });

    expect(result.current.isNotFound).toBe(false);
    expect(result.current.problem).toMatchObject({
      title: "Request failed",
      detail: "Asset lookup timeout",
    });
  });
});
