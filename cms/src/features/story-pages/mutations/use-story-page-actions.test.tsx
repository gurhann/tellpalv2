import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook } from "@testing-library/react";
import type { PropsWithChildren } from "react";
import { act } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import type { AdminStoryPageResponse } from "@/features/contents/api/story-page-admin";
import { storyContentViewModel } from "@/features/contents/test/fixtures";
import { queryKeys } from "@/lib/query-keys";

import { useStoryPageActions } from "./use-story-page-actions";

const storyPageAdminApiMock = vi.hoisted(() => ({
  addStoryPage: vi.fn(),
  updateStoryPage: vi.fn(),
  removeStoryPage: vi.fn(),
}));

vi.mock("@/features/contents/api/story-page-admin", async () => {
  const actual = await vi.importActual<
    typeof import("@/features/contents/api/story-page-admin")
  >("@/features/contents/api/story-page-admin");

  return {
    ...actual,
    storyPageAdminApi: {
      ...actual.storyPageAdminApi,
      addStoryPage: storyPageAdminApiMock.addStoryPage,
      updateStoryPage: storyPageAdminApiMock.updateStoryPage,
      removeStoryPage: storyPageAdminApiMock.removeStoryPage,
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
  storyPageAdminApiMock.addStoryPage.mockReset();
  storyPageAdminApiMock.updateStoryPage.mockReset();
  storyPageAdminApiMock.removeStoryPage.mockReset();
});

describe("useStoryPageActions", () => {
  it("creates a story page and invalidates related content queries", async () => {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
      },
    });
    const createdStoryPage: AdminStoryPageResponse = {
      contentId: 1,
      pageNumber: 3,
      illustrationMediaId: null,
      localizationCount: 0,
    };
    const invalidateQueries = vi.spyOn(queryClient, "invalidateQueries");

    storyPageAdminApiMock.addStoryPage.mockResolvedValue(createdStoryPage);

    const { result } = renderHook(() => useStoryPageActions({ contentId: 1 }), {
      wrapper: createWrapper(queryClient),
    });

    await act(async () => {
      await result.current.addStoryPage.mutateAsync({
        pageNumber: 3,
        illustrationMediaId: null,
      });
    });

    expect(storyPageAdminApiMock.addStoryPage).toHaveBeenCalledWith(1, {
      pageNumber: 3,
      illustrationMediaId: null,
    });
    expect(invalidateQueries).toHaveBeenCalledWith({
      queryKey: queryKeys.contents.storyPages(1),
    });
    expect(invalidateQueries).toHaveBeenCalledWith({
      queryKey: queryKeys.contents.detail(1),
    });
    expect(invalidateQueries).toHaveBeenCalledWith({
      queryKey: queryKeys.contents.lists(),
    });
    expect(invalidateQueries).toHaveBeenCalledWith({
      queryKey: queryKeys.contents.storyPage(1, 3),
    });
  });

  it("updates story page metadata and invalidates list plus detail keys", async () => {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
      },
    });
    const updatedStoryPage: AdminStoryPageResponse = {
      contentId: 1,
      pageNumber: 1,
      illustrationMediaId: 55,
      localizationCount: 2,
    };
    const invalidateQueries = vi.spyOn(queryClient, "invalidateQueries");

    queryClient.setQueryData(
      queryKeys.contents.detail(1),
      storyContentViewModel,
    );
    storyPageAdminApiMock.updateStoryPage.mockResolvedValue(updatedStoryPage);

    const { result } = renderHook(() => useStoryPageActions({ contentId: 1 }), {
      wrapper: createWrapper(queryClient),
    });

    await act(async () => {
      await result.current.updateStoryPage.mutateAsync({
        pageNumber: 1,
        input: {
          illustrationMediaId: 55,
        },
      });
    });

    expect(storyPageAdminApiMock.updateStoryPage).toHaveBeenCalledWith(1, 1, {
      illustrationMediaId: 55,
    });
    expect(invalidateQueries).toHaveBeenCalledWith({
      queryKey: queryKeys.contents.storyPages(1),
    });
    expect(invalidateQueries).toHaveBeenCalledWith({
      queryKey: queryKeys.contents.storyPage(1, 1),
    });
  });

  it("deletes a story page and invalidates related content caches", async () => {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
      },
    });
    const invalidateQueries = vi.spyOn(queryClient, "invalidateQueries");

    storyPageAdminApiMock.removeStoryPage.mockResolvedValue(undefined);

    const { result } = renderHook(() => useStoryPageActions({ contentId: 1 }), {
      wrapper: createWrapper(queryClient),
    });

    await act(async () => {
      await result.current.removeStoryPage.mutateAsync(2);
    });

    expect(storyPageAdminApiMock.removeStoryPage).toHaveBeenCalledWith(1, 2);
    expect(invalidateQueries).toHaveBeenCalledWith({
      queryKey: queryKeys.contents.storyPages(1),
    });
    expect(invalidateQueries).toHaveBeenCalledWith({
      queryKey: queryKeys.contents.storyPage(1, 2),
    });
  });
});
