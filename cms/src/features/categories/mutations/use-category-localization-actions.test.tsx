import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook } from "@testing-library/react";
import type { PropsWithChildren } from "react";
import { act } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import {
  categoryLocalizationResponses,
  featuredSleepEnglishLocalizationViewModel,
} from "@/features/categories/test/fixtures";
import { queryKeys } from "@/lib/query-keys";

import { useCategoryLocalizationActions } from "./use-category-localization-actions";

const categoryAdminApiMock = vi.hoisted(() => ({
  createLocalization: vi.fn(),
  updateLocalization: vi.fn(),
}));

vi.mock("@/features/categories/api/category-admin", async () => {
  const actual = await vi.importActual<
    typeof import("@/features/categories/api/category-admin")
  >("@/features/categories/api/category-admin");

  return {
    ...actual,
    categoryAdminApi: {
      ...actual.categoryAdminApi,
      createLocalization: categoryAdminApiMock.createLocalization,
      updateLocalization: categoryAdminApiMock.updateLocalization,
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
  categoryAdminApiMock.createLocalization.mockReset();
  categoryAdminApiMock.updateLocalization.mockReset();
});

describe("useCategoryLocalizationActions", () => {
  it("creates a localization and updates the session-backed localization cache", async () => {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
      },
    });
    const invalidateQueries = vi.spyOn(queryClient, "invalidateQueries");
    const createdLocalization = categoryLocalizationResponses[0];

    categoryAdminApiMock.createLocalization.mockResolvedValue(
      createdLocalization,
    );

    const { result } = renderHook(() => useCategoryLocalizationActions(7), {
      wrapper: createWrapper(queryClient),
    });

    await act(async () => {
      await result.current.saveLocalization.mutateAsync({
        mode: "create",
        values: {
          languageCode: "en",
          name: "Featured Sleep",
          description: "Curated sleep stories for bedtime.",
          imageMediaId: 4,
          status: "PUBLISHED",
          publishedAt: "2026-03-29T14:00",
        },
      });
    });

    expect(categoryAdminApiMock.createLocalization).toHaveBeenCalledWith(
      7,
      "en",
      {
        name: "Featured Sleep",
        description: "Curated sleep stories for bedtime.",
        imageMediaId: 4,
        status: "PUBLISHED",
        publishedAt: "2026-03-29T11:00:00.000Z",
      },
    );
    expect(
      queryClient.getQueryData<Array<{ languageCode: string }>>(
        queryKeys.categories.localizations(7),
      ),
    ).toEqual(
      expect.arrayContaining([
        expect.objectContaining({
          languageCode: "en",
        }),
      ]),
    );
    expect(invalidateQueries).toHaveBeenCalledWith({
      queryKey: queryKeys.categories.localizations(7),
    });
    expect(invalidateQueries).toHaveBeenCalledWith({
      queryKey: queryKeys.categories.localization(7, "en"),
    });
  });

  it("updates an existing localization in the session-backed cache", async () => {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
      },
    });
    const updatedLocalization = {
      ...categoryLocalizationResponses[0],
      name: "Featured Sleep Updated",
      description: "Updated category description.",
      status: "DRAFT" as const,
      publishedAt: null,
      published: false,
    };

    queryClient.setQueryData(queryKeys.categories.localizations(7), [
      featuredSleepEnglishLocalizationViewModel,
    ]);
    categoryAdminApiMock.updateLocalization.mockResolvedValue(
      updatedLocalization,
    );

    const { result } = renderHook(() => useCategoryLocalizationActions(7), {
      wrapper: createWrapper(queryClient),
    });

    await act(async () => {
      await result.current.saveLocalization.mutateAsync({
        mode: "update",
        values: {
          languageCode: "en",
          name: "Featured Sleep Updated",
          description: "Updated category description.",
          imageMediaId: 4,
          status: "DRAFT",
          publishedAt: null,
        },
      });
    });

    expect(categoryAdminApiMock.updateLocalization).toHaveBeenCalledWith(
      7,
      "en",
      {
        name: "Featured Sleep Updated",
        description: "Updated category description.",
        imageMediaId: 4,
        status: "DRAFT",
        publishedAt: null,
      },
    );
    expect(
      queryClient.getQueryData<Array<{ languageCode: string; name: string }>>(
        queryKeys.categories.localizations(7),
      ),
    ).toEqual(
      expect.arrayContaining([
        expect.objectContaining({
          languageCode: "en",
          name: "Featured Sleep Updated",
        }),
      ]),
    );
  });
});
