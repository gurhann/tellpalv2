import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import type { PropsWithChildren } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { assetResponses } from "@/features/assets/test/fixtures";
import { ApiClientError } from "@/lib/http/client";
import type { ApiProblemDetail } from "@/types/api";

import { useRecentAssets } from "./use-recent-assets";

const assetAdminApiMock = vi.hoisted(() => ({
  listRecentAssets: vi.fn(),
}));

vi.mock("@/features/assets/api/asset-admin", async () => {
  const actual = await vi.importActual<
    typeof import("@/features/assets/api/asset-admin")
  >("@/features/assets/api/asset-admin");

  return {
    ...actual,
    assetAdminApi: {
      ...actual.assetAdminApi,
      listRecentAssets: assetAdminApiMock.listRecentAssets,
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
    status: 503,
    detail: "Asset service unavailable",
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
  assetAdminApiMock.listRecentAssets.mockReset();
});

describe("useRecentAssets", () => {
  it("loads and maps the recent asset list with the provided limit", async () => {
    assetAdminApiMock.listRecentAssets.mockResolvedValue(assetResponses);

    const { result } = renderHook(() => useRecentAssets(12), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(assetAdminApiMock.listRecentAssets).toHaveBeenCalledWith(12);
    expect(result.current.limit).toBe(12);
    expect(result.current.assets).toHaveLength(3);
    expect(result.current.assets[0]?.kindLabel).toBe("Phone Thumbnail");
    expect(result.current.problem).toBeNull();
  });

  it("returns an empty asset collection when the backend has no records", async () => {
    assetAdminApiMock.listRecentAssets.mockResolvedValue([]);

    const { result } = renderHook(() => useRecentAssets(), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(result.current.assets).toEqual([]);
    expect(result.current.problem).toBeNull();
  });

  it("passes through API problem detail on asset list failures", async () => {
    assetAdminApiMock.listRecentAssets.mockRejectedValue(
      makeApiClientError(
        makeProblem({
          title: "Asset registry unavailable",
          detail: "The asset service timed out.",
        }),
      ),
    );

    const { result } = renderHook(() => useRecentAssets(), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });

    expect(result.current.problem).toMatchObject({
      status: 503,
      title: "Asset registry unavailable",
      detail: "The asset service timed out.",
    });
  });
});
