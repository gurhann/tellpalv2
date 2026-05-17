import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook } from "@testing-library/react";
import type { PropsWithChildren } from "react";
import { act } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import type { AdminStoryPageResponse } from "@/features/contents/api/story-page-admin";
import { queryKeys } from "@/lib/query-keys";

import { useStoryPageActions } from "./use-story-page-actions";

const storyPageAdminApiMock = vi.hoisted(() => ({
  addStoryPage: vi.fn(),
  updateStoryPage: vi.fn(),
  removeStoryPage: vi.fn(),
  upsertStoryPageLocalization: vi.fn(),
  exportTextlessIllustrations: vi.fn(),
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
      exportTextlessIllustrations:
        storyPageAdminApiMock.exportTextlessIllustrations,
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
  storyPageAdminApiMock.updateStoryPage.mockReset();
  storyPageAdminApiMock.removeStoryPage.mockReset();
  storyPageAdminApiMock.exportTextlessIllustrations.mockReset();
  storyPageAdminApiMock.upsertStoryPageLocalization.mockReset();
});

afterEach(() => {
  vi.restoreAllMocks();
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
      textlessIllustrationMediaId: null,
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

  it("updates page-level source image and invalidates story page caches", async () => {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
      },
    });
    const updatedStoryPage: AdminStoryPageResponse = {
      contentId: 1,
      pageNumber: 2,
      textlessIllustrationMediaId: 91,
      localizationCount: 1,
    };
    const invalidateQueries = vi.spyOn(queryClient, "invalidateQueries");

    storyPageAdminApiMock.updateStoryPage.mockResolvedValue(updatedStoryPage);

    const { result } = renderHook(() => useStoryPageActions({ contentId: 1 }), {
      wrapper: createWrapper(queryClient),
    });

    await act(async () => {
      await result.current.updateStoryPage.mutateAsync({
        pageNumber: 2,
        input: {
          textlessIllustrationMediaId: 91,
        },
      });
    });

    expect(storyPageAdminApiMock.updateStoryPage).toHaveBeenCalledWith(1, 2, {
      textlessIllustrationMediaId: 91,
    });
    expect(invalidateQueries).toHaveBeenCalledWith({
      queryKey: queryKeys.contents.storyPages(1),
    });
    expect(invalidateQueries).toHaveBeenCalledWith({
      queryKey: queryKeys.contents.storyPage(1, 2),
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

  it("downloads source image export as a zip file", async () => {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
      },
    });
    const createObjectURL = vi.fn(() => "blob:story-export");
    const revokeObjectURL = vi.fn();
    const click = vi.fn();
    const append = vi.spyOn(document.body, "append");
    const anchor = document.createElement("a");
    vi.spyOn(document, "createElement").mockReturnValue(anchor);
    anchor.click = click;
    Object.defineProperty(URL, "createObjectURL", {
      configurable: true,
      value: createObjectURL,
    });
    Object.defineProperty(URL, "revokeObjectURL", {
      configurable: true,
      value: revokeObjectURL,
    });
    storyPageAdminApiMock.exportTextlessIllustrations.mockResolvedValue({
      blob: new Blob(["zip"], { type: "application/zip" }),
      fileName: "story-source-images.zip",
    });

    const { result } = renderHook(() => useStoryPageActions({ contentId: 1 }), {
      wrapper: createWrapper(queryClient),
    });

    await act(async () => {
      await result.current.exportTextlessIllustrations.mutateAsync();
    });

    expect(
      storyPageAdminApiMock.exportTextlessIllustrations,
    ).toHaveBeenCalledWith(1);
    expect(append).toHaveBeenCalledWith(anchor);
    expect(anchor.download).toBe("story-source-images.zip");
    expect(click).toHaveBeenCalled();
    expect(revokeObjectURL).toHaveBeenCalledWith("blob:story-export");
  });
});
