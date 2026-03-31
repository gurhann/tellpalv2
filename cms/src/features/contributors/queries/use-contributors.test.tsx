import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import type { PropsWithChildren } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { contributorResponses } from "@/features/contributors/test/fixtures";
import { ApiClientError } from "@/lib/http/client";
import type { ApiProblemDetail } from "@/types/api";

import { useContributors } from "./use-contributors";

const contributorAdminApiMock = vi.hoisted(() => ({
  listContributors: vi.fn(),
}));

vi.mock("@/features/contributors/api/contributor-admin", async () => {
  const actual = await vi.importActual<
    typeof import("@/features/contributors/api/contributor-admin")
  >("@/features/contributors/api/contributor-admin");

  return {
    ...actual,
    contributorAdminApi: {
      ...actual.contributorAdminApi,
      listContributors: contributorAdminApiMock.listContributors,
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
    detail: "Contributor service unavailable",
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
  contributorAdminApiMock.listContributors.mockReset();
});

describe("useContributors", () => {
  it("loads and maps the recent contributor list with the provided limit", async () => {
    contributorAdminApiMock.listContributors.mockResolvedValue(
      contributorResponses,
    );

    const { result } = renderHook(() => useContributors(12), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(contributorAdminApiMock.listContributors).toHaveBeenCalledWith(12);
    expect(result.current.limit).toBe(12);
    expect(result.current.contributors).toHaveLength(3);
    expect(result.current.contributors[0]?.displayName).toBe("Annie Case");
    expect(result.current.problem).toBeNull();
  });

  it("returns an empty contributor collection when the backend has no records", async () => {
    contributorAdminApiMock.listContributors.mockResolvedValue([]);

    const { result } = renderHook(() => useContributors(8), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(result.current.contributors).toEqual([]);
    expect(result.current.problem).toBeNull();
  });

  it("passes through API problem detail on contributor list failures", async () => {
    contributorAdminApiMock.listContributors.mockRejectedValue(
      makeApiClientError(
        makeProblem({
          title: "Contributor registry unavailable",
          detail: "The contributor service timed out.",
        }),
      ),
    );

    const { result } = renderHook(() => useContributors(), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });

    expect(result.current.problem).toMatchObject({
      status: 503,
      title: "Contributor registry unavailable",
      detail: "The contributor service timed out.",
    });
  });
});
