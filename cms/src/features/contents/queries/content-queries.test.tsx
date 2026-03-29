import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import type { PropsWithChildren } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import {
  contentReadResponses,
  storyContentReadResponse,
} from "@/features/contents/test/fixtures";
import { ApiClientError } from "@/lib/http/client";
import type { ApiProblemDetail } from "@/types/api";

import { useContentDetail } from "./use-content-detail";
import { useContentList } from "./use-content-list";

const contentAdminApiMock = vi.hoisted(() => ({
  listContents: vi.fn(),
  getContent: vi.fn(),
}));

vi.mock("@/features/contents/api/content-admin", () => ({
  contentAdminApi: contentAdminApiMock,
}));

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
  contentAdminApiMock.listContents.mockReset();
  contentAdminApiMock.getContent.mockReset();
});

describe("content queries", () => {
  it("loads and maps the content registry", async () => {
    contentAdminApiMock.listContents.mockResolvedValue(contentReadResponses);

    const { result } = renderHook(() => useContentList(), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(result.current.contents).toHaveLength(3);
    expect(result.current.contents[0]?.summary.externalKey).toBe(
      "story.evening-garden",
    );
    expect(result.current.contents[0]?.localizations).toHaveLength(2);
    expect(result.current.problem).toBeNull();
  });

  it("returns an empty registry when the backend has no content records", async () => {
    contentAdminApiMock.listContents.mockResolvedValue([]);

    const { result } = renderHook(() => useContentList(), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(result.current.contents).toEqual([]);
    expect(result.current.problem).toBeNull();
  });

  it("surfaces a generic problem for unexpected list failures", async () => {
    contentAdminApiMock.listContents.mockRejectedValue(
      new Error("Network down"),
    );

    const { result } = renderHook(() => useContentList(), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });

    expect(result.current.problem).toMatchObject({
      title: "Request failed",
      detail: "Network down",
    });
  });

  it("loads and maps content detail records", async () => {
    contentAdminApiMock.getContent.mockResolvedValue(storyContentReadResponse);

    const { result } = renderHook(() => useContentDetail(1), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(result.current.content?.summary.externalKey).toBe(
      "story.evening-garden",
    );
    expect(result.current.content?.primaryLocalization?.title).toBe(
      "Evening Garden",
    );
    expect(result.current.isNotFound).toBe(false);
  });

  it("marks content detail queries as not found for content_not_found responses", async () => {
    contentAdminApiMock.getContent.mockRejectedValue(
      makeApiClientError(
        makeProblem({
          status: 404,
          title: "Content not found",
          detail: "Content 999 was not found",
          errorCode: "content_not_found",
          path: "/api/admin/contents/999",
        }),
      ),
    );

    const { result } = renderHook(() => useContentDetail(999), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });

    expect(result.current.isNotFound).toBe(true);
    expect(result.current.problem?.status).toBe(404);
  });

  it("keeps generic detail failures separate from not-found handling", async () => {
    contentAdminApiMock.getContent.mockRejectedValue(
      new Error("Backend timeout"),
    );

    const { result } = renderHook(() => useContentDetail(2), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });

    expect(result.current.isNotFound).toBe(false);
    expect(result.current.problem).toMatchObject({
      title: "Request failed",
      detail: "Backend timeout",
    });
  });
});
