import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import type { PropsWithChildren } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { contentContributorResponses } from "@/features/contributors/test/fixtures";
import { ApiClientError } from "@/lib/http/client";
import type { ApiProblemDetail } from "@/types/api";

import { useContentContributorAssignments } from "./use-content-contributor-assignments";

const contributorAdminApiMock = vi.hoisted(() => ({
  listContentContributors: vi.fn(),
}));

vi.mock("@/features/contributors/api/contributor-admin", async () => {
  const actual = await vi.importActual<
    typeof import("@/features/contributors/api/contributor-admin")
  >("@/features/contributors/api/contributor-admin");

  return {
    ...actual,
    contributorAdminApi: {
      ...actual.contributorAdminApi,
      listContentContributors: contributorAdminApiMock.listContentContributors,
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
    detail: "Contributor assignments unavailable",
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
  contributorAdminApiMock.listContentContributors.mockReset();
});

describe("useContentContributorAssignments", () => {
  it("loads and maps persisted content contributor assignments", async () => {
    contributorAdminApiMock.listContentContributors.mockResolvedValue(
      contentContributorResponses,
    );

    const { result } = renderHook(
      () => useContentContributorAssignments(1),
      {
        wrapper: createWrapper(),
      },
    );

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(contributorAdminApiMock.listContentContributors).toHaveBeenCalledWith(
      1,
    );
    expect(result.current.assignments[0]?.languageLabel).toBe("English");
    expect(result.current.problem).toBeNull();
  });

  it("surfaces API problem details on assignment read failures", async () => {
    contributorAdminApiMock.listContentContributors.mockRejectedValue(
      makeApiClientError(
        makeProblem({
          title: "Contributor assignments unavailable",
          detail: "The content contributor read endpoint timed out.",
        }),
      ),
    );

    const { result } = renderHook(
      () => useContentContributorAssignments(1),
      {
        wrapper: createWrapper(),
      },
    );

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });

    expect(result.current.problem).toMatchObject({
      status: 503,
      title: "Contributor assignments unavailable",
      detail: "The content contributor read endpoint timed out.",
    });
  });
});
