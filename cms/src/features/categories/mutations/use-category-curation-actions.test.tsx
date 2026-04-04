import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook } from "@testing-library/react";
import type { PropsWithChildren } from "react";
import { act } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import type { AdminCategoryContentResponse } from "@/features/categories/api/category-curation-admin";
import { categoryCurationItemViewModels } from "@/features/categories/test/fixtures";
import { queryKeys } from "@/lib/query-keys";

import { useCategoryCurationActions } from "./use-category-curation-actions";

const categoryCurationAdminApiMock = vi.hoisted(() => ({
  addCuratedContent: vi.fn(),
  updateCuratedContentOrder: vi.fn(),
}));

vi.mock("@/features/categories/api/category-curation-admin", async () => {
  const actual = await vi.importActual<
    typeof import("@/features/categories/api/category-curation-admin")
  >("@/features/categories/api/category-curation-admin");

  return {
    ...actual,
    categoryCurationAdminApi: {
      ...actual.categoryCurationAdminApi,
      addCuratedContent: categoryCurationAdminApiMock.addCuratedContent,
      updateCuratedContentOrder:
        categoryCurationAdminApiMock.updateCuratedContentOrder,
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
  categoryCurationAdminApiMock.addCuratedContent.mockReset();
  categoryCurationAdminApiMock.updateCuratedContentOrder.mockReset();
});

describe("useCategoryCurationActions", () => {
  it("adds a curated item and stores it in the session-backed cache", async () => {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
      },
    });
    const addedItem: AdminCategoryContentResponse = {
      categoryId: 7,
      languageCode: "en",
      contentId: 1,
      displayOrder: 0,
    };
    const invalidateQueries = vi.spyOn(queryClient, "invalidateQueries");

    queryClient.setQueryData(queryKeys.categories.curation(7, "en"), [
      categoryCurationItemViewModels[1],
    ]);
    categoryCurationAdminApiMock.addCuratedContent.mockResolvedValue(addedItem);

    const { result } = renderHook(
      () =>
        useCategoryCurationActions({
          categoryId: 7,
          languageCode: "en",
        }),
      {
        wrapper: createWrapper(queryClient),
      },
    );

    await act(async () => {
      await result.current.addCuratedContent.mutateAsync({
        contentId: 1,
        displayOrder: 0,
      });
    });

    expect(categoryCurationAdminApiMock.addCuratedContent).toHaveBeenCalledWith(
      7,
      "en",
      {
        contentId: 1,
        displayOrder: 0,
      },
    );
    expect(
      queryClient.getQueryData<
        Array<{ contentId: number; displayOrder: number }>
      >(queryKeys.categories.curation(7, "en")),
    ).toEqual([
      expect.objectContaining({
        contentId: 1,
        displayOrder: 0,
      }),
      expect.objectContaining({
        contentId: 10,
        displayOrder: 2,
      }),
    ]);
    expect(invalidateQueries).toHaveBeenCalledWith({
      queryKey: queryKeys.categories.curation(7, "en"),
    });
  });

  it("updates display order and keeps the cache sorted", async () => {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
      },
    });
    const updatedItem: AdminCategoryContentResponse = {
      categoryId: 7,
      languageCode: "en",
      contentId: 10,
      displayOrder: 1,
    };

    queryClient.setQueryData(
      queryKeys.categories.curation(7, "en"),
      categoryCurationItemViewModels,
    );
    categoryCurationAdminApiMock.updateCuratedContentOrder.mockResolvedValue(
      updatedItem,
    );

    const { result } = renderHook(
      () =>
        useCategoryCurationActions({
          categoryId: 7,
          languageCode: "en",
        }),
      {
        wrapper: createWrapper(queryClient),
      },
    );

    await act(async () => {
      await result.current.updateCuratedContentOrder.mutateAsync({
        contentId: 10,
        displayOrder: 1,
      });
    });

    expect(
      categoryCurationAdminApiMock.updateCuratedContentOrder,
    ).toHaveBeenCalledWith(7, "en", 10, {
      displayOrder: 1,
    });
    expect(
      queryClient.getQueryData<
        Array<{ contentId: number; displayOrder: number }>
      >(queryKeys.categories.curation(7, "en")),
    ).toEqual([
      expect.objectContaining({
        contentId: 1,
        displayOrder: 0,
      }),
      expect.objectContaining({
        contentId: 10,
        displayOrder: 1,
      }),
    ]);
  });
});
