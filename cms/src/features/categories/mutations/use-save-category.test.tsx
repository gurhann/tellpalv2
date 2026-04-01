import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook } from "@testing-library/react";
import type { PropsWithChildren } from "react";
import { act } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import type { AdminCategoryResponse } from "@/features/categories/api/category-admin";
import {
  archivedCategoryViewModel,
  featuredSleepCategoryViewModel,
} from "@/features/categories/test/fixtures";
import { queryKeys } from "@/lib/query-keys";

import { useSaveCategory } from "./use-save-category";

const categoryAdminApiMock = vi.hoisted(() => ({
  createCategory: vi.fn(),
  updateCategory: vi.fn(),
}));

vi.mock("@/features/categories/api/category-admin", async () => {
  const actual = await vi.importActual<
    typeof import("@/features/categories/api/category-admin")
  >("@/features/categories/api/category-admin");

  return {
    ...actual,
    categoryAdminApi: {
      ...actual.categoryAdminApi,
      createCategory: categoryAdminApiMock.createCategory,
      updateCategory: categoryAdminApiMock.updateCategory,
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
  categoryAdminApiMock.createCategory.mockReset();
  categoryAdminApiMock.updateCategory.mockReset();
});

describe("useSaveCategory", () => {
  it("creates a category and updates list/detail caches", async () => {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
      },
    });
    const createdCategory: AdminCategoryResponse = {
      categoryId: 99,
      type: "CONTENT",
      slug: "featured-breathing",
      premium: false,
      active: true,
    };
    const onSuccess = vi.fn();
    const invalidateQueries = vi.spyOn(queryClient, "invalidateQueries");

    queryClient.setQueryData(queryKeys.categories.list(), [
      featuredSleepCategoryViewModel,
    ]);
    categoryAdminApiMock.createCategory.mockResolvedValue(createdCategory);

    const { result } = renderHook(
      () =>
        useSaveCategory({
          mode: "create",
          onSuccess,
        }),
      {
        wrapper: createWrapper(queryClient),
      },
    );

    await act(async () => {
      await result.current.mutateAsync({
        type: "CONTENT",
        slug: "featured-breathing",
        premium: false,
        active: true,
      });
    });

    expect(categoryAdminApiMock.createCategory).toHaveBeenCalledWith({
      type: "CONTENT",
      slug: "featured-breathing",
      premium: false,
      active: true,
    });
    expect(
      queryClient.getQueryData(queryKeys.categories.detail(99)),
    ).toMatchObject({
      id: 99,
      slug: "featured-breathing",
      type: "CONTENT",
    });
    expect(
      queryClient.getQueryData<Array<{ id: number }>>(
        queryKeys.categories.list(),
      ),
    ).toEqual(expect.arrayContaining([expect.objectContaining({ id: 99 })]));
    expect(onSuccess).toHaveBeenCalledWith(createdCategory);
    expect(invalidateQueries).toHaveBeenCalledWith({
      queryKey: queryKeys.categories.lists(),
    });
    expect(invalidateQueries).toHaveBeenCalledWith({
      queryKey: queryKeys.categories.detail(99),
    });
  });

  it("updates category metadata and refreshes list/detail caches", async () => {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
      },
    });
    const updatedCategory: AdminCategoryResponse = {
      categoryId: 7,
      type: "CONTENT",
      slug: "featured-sleep-updated",
      premium: true,
      active: false,
    };

    queryClient.setQueryData(queryKeys.categories.list(), [
      featuredSleepCategoryViewModel,
      archivedCategoryViewModel,
    ]);
    queryClient.setQueryData(
      queryKeys.categories.detail(7),
      featuredSleepCategoryViewModel,
    );
    categoryAdminApiMock.updateCategory.mockResolvedValue(updatedCategory);

    const { result } = renderHook(
      () =>
        useSaveCategory({
          mode: "update",
          categoryId: 7,
        }),
      {
        wrapper: createWrapper(queryClient),
      },
    );

    await act(async () => {
      await result.current.mutateAsync({
        type: "CONTENT",
        slug: "featured-sleep-updated",
        premium: true,
        active: false,
      });
    });

    expect(categoryAdminApiMock.updateCategory).toHaveBeenCalledWith(7, {
      type: "CONTENT",
      slug: "featured-sleep-updated",
      premium: true,
      active: false,
    });
    expect(
      queryClient.getQueryData<{
        slug: string;
        premium: boolean;
        active: boolean;
      }>(queryKeys.categories.detail(7)),
    ).toMatchObject({
      slug: "featured-sleep-updated",
      premium: true,
      active: false,
    });
  });
});
