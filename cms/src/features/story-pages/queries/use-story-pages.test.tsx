import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import type { PropsWithChildren } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { storyPageReadResponses } from "@/features/story-pages/test/fixtures";
import { ApiClientError } from "@/lib/http/client";
import type { ApiProblemDetail } from "@/types/api";

import { useStoryPage, useStoryPages } from "./use-story-pages";

const storyPageAdminApiMock = vi.hoisted(() => ({
  listStoryPages: vi.fn(),
  getStoryPage: vi.fn(),
}));

vi.mock("@/features/contents/api/story-page-admin", async () => {
  const actual = await vi.importActual<
    typeof import("@/features/contents/api/story-page-admin")
  >("@/features/contents/api/story-page-admin");

  return {
    ...actual,
    storyPageAdminApi: {
      ...actual.storyPageAdminApi,
      listStoryPages: storyPageAdminApiMock.listStoryPages,
      getStoryPage: storyPageAdminApiMock.getStoryPage,
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
    status: 500,
    detail: "Unexpected backend error",
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
  storyPageAdminApiMock.listStoryPages.mockReset();
  storyPageAdminApiMock.getStoryPage.mockReset();
});

describe("story page queries", () => {
  it("loads and maps the story page collection", async () => {
    storyPageAdminApiMock.listStoryPages.mockResolvedValue(
      storyPageReadResponses,
    );

    const { result } = renderHook(() => useStoryPages(1), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(result.current.storyPages).toHaveLength(2);
    expect(result.current.storyPages[0]?.pageNumber).toBe(1);
    expect(result.current.storyPages[0]?.languageCodes).toEqual(["en", "tr"]);
    expect(result.current.problem).toBeNull();
  });

  it("returns an empty collection when no story pages exist yet", async () => {
    storyPageAdminApiMock.listStoryPages.mockResolvedValue([]);

    const { result } = renderHook(() => useStoryPages(1), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(result.current.storyPages).toEqual([]);
    expect(result.current.problem).toBeNull();
  });

  it("surfaces generic list problems for unexpected failures", async () => {
    storyPageAdminApiMock.listStoryPages.mockRejectedValue(
      new Error("Story page read timed out"),
    );

    const { result } = renderHook(() => useStoryPages(1), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });

    expect(result.current.problem).toMatchObject({
      title: "Request failed",
      detail: "Story page read timed out",
    });
  });

  it("loads one story page detail record when requested directly", async () => {
    storyPageAdminApiMock.getStoryPage.mockResolvedValue(
      storyPageReadResponses[0],
    );

    const { result } = renderHook(() => useStoryPage(1, 1), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(result.current.storyPage?.pageNumber).toBe(1);
    expect(result.current.storyPage?.localizations).toHaveLength(2);
  });

  it("passes through API problem detail on story page detail errors", async () => {
    storyPageAdminApiMock.getStoryPage.mockRejectedValue(
      makeApiClientError(
        makeProblem({
          status: 404,
          title: "Story page not found",
          detail: "Page 9 does not exist.",
          errorCode: "story_page_not_found",
          path: "/api/admin/contents/1/story-pages/9",
        }),
      ),
    );

    const { result } = renderHook(() => useStoryPage(1, 9), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });

    expect(result.current.problem).toMatchObject({
      status: 404,
      errorCode: "story_page_not_found",
    });
  });
});
