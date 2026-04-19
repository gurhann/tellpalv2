import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook } from "@testing-library/react";
import type { PropsWithChildren } from "react";
import { act } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import type { AdminStoryPageResponse } from "@/features/contents/api/story-page-admin";
import { queryKeys } from "@/lib/query-keys";

import { useStoryPageActions } from "./use-story-page-actions";

const storyPageAdminApiMock = vi.hoisted(() => ({
  addStoryPage: vi.fn(),
  removeStoryPage: vi.fn(),
  upsertStoryPageLocalization: vi.fn(),
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
      removeStoryPage: storyPageAdminApiMock.removeStoryPage,
      upsertStoryPageLocalization:
        storyPageAdminApiMock.upsertStoryPageLocalization,
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
  storyPageAdminApiMock.removeStoryPage.mockReset();
  storyPageAdminApiMock.upsertStoryPageLocalization.mockReset();
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
      localizationCount: 0,
    };
    const invalidateQueries = vi.spyOn(queryClient, "invalidateQueries");

    storyPageAdminApiMock.addStoryPage.mockResolvedValue(createdStoryPage);

    const { result } = renderHook(() => useStoryPageActions({ contentId: 1 }), {
      wrapper: createWrapper(queryClient),
    });

    await act(async () => {
      await result.current.addStoryPage.mutateAsync({
        afterPageNumber: 2,
      });
    });

    expect(storyPageAdminApiMock.addStoryPage).toHaveBeenCalledWith(1, {
      afterPageNumber: 2,
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

  it("upserts a story page localization and invalidates localized story queries", async () => {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
      },
    });
    const invalidateQueries = vi.spyOn(queryClient, "invalidateQueries");

    storyPageAdminApiMock.upsertStoryPageLocalization.mockResolvedValue({
      contentId: 1,
      pageNumber: 1,
      languageCode: "en",
      bodyText: "Fireflies drift over the gate.",
      audioMediaId: 3,
      illustrationMediaId: 41,
    });

    const { result } = renderHook(() => useStoryPageActions({ contentId: 1 }), {
      wrapper: createWrapper(queryClient),
    });

    await act(async () => {
      await result.current.upsertStoryPageLocalization.mutateAsync({
        pageNumber: 1,
        languageCode: "en",
        input: {
          bodyText: "Fireflies drift over the gate.",
          audioMediaId: 3,
          illustrationMediaId: 41,
        },
      });
    });

    expect(
      storyPageAdminApiMock.upsertStoryPageLocalization,
    ).toHaveBeenCalledWith(1, 1, "en", {
      bodyText: "Fireflies drift over the gate.",
      audioMediaId: 3,
      illustrationMediaId: 41,
    });
    expect(invalidateQueries).toHaveBeenCalledWith({
      queryKey: queryKeys.contents.storyPages(1),
    });
    expect(invalidateQueries).toHaveBeenCalledWith({
      queryKey: queryKeys.contents.storyPage(1, 1),
    });
    expect(invalidateQueries).toHaveBeenCalledWith({
      queryKey: queryKeys.contents.storyPageLocalization(1, 1, "en"),
    });
  });
});
