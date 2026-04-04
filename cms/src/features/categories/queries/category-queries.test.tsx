import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import type { PropsWithChildren } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import {
  categoryCurationResponses,
  categoryLocalizationResponses,
  categoryResponses,
  featuredSleepCategoryResponse,
} from "@/features/categories/test/fixtures";
import { ApiClientError } from "@/lib/http/client";
import type { ApiProblemDetail } from "@/types/api";

import { useCategoryDetail } from "./use-category-detail";
import { useCategoryCuration } from "./use-category-curation";
import { useCategoryList } from "./use-category-list";
import { useCategoryLocalizations } from "./use-category-localizations";

const categoryAdminApiMock = vi.hoisted(() => ({
  listCategories: vi.fn(),
  getCategory: vi.fn(),
  listLocalizations: vi.fn(),
}));

const categoryCurationAdminApiMock = vi.hoisted(() => ({
  listCuratedContent: vi.fn(),
}));

vi.mock("@/features/categories/api/category-admin", () => ({
  categoryAdminApi: categoryAdminApiMock,
}));

vi.mock("@/features/categories/api/category-curation-admin", async () => {
  const actual = await vi.importActual<
    typeof import("@/features/categories/api/category-curation-admin")
  >("@/features/categories/api/category-curation-admin");

  return {
    ...actual,
    categoryCurationAdminApi: {
      ...actual.categoryCurationAdminApi,
      listCuratedContent: categoryCurationAdminApiMock.listCuratedContent,
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
  categoryAdminApiMock.listCategories.mockReset();
  categoryAdminApiMock.getCategory.mockReset();
  categoryAdminApiMock.listLocalizations.mockReset();
  categoryCurationAdminApiMock.listCuratedContent.mockReset();
});

describe("category queries", () => {
  it("loads and maps the category registry", async () => {
    categoryAdminApiMock.listCategories.mockResolvedValue(categoryResponses);

    const { result } = renderHook(() => useCategoryList(), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(result.current.categories).toHaveLength(3);
    expect(result.current.categories[0]?.slug).toBe("featured-sleep");
    expect(result.current.categories[1]?.typeLabel).toBe("Meditation");
    expect(result.current.problem).toBeNull();
  });

  it("returns an empty registry when the backend has no categories", async () => {
    categoryAdminApiMock.listCategories.mockResolvedValue([]);

    const { result } = renderHook(() => useCategoryList(), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(result.current.categories).toEqual([]);
    expect(result.current.problem).toBeNull();
  });

  it("surfaces a generic problem for unexpected list failures", async () => {
    categoryAdminApiMock.listCategories.mockRejectedValue(
      new Error("Registry timeout"),
    );

    const { result } = renderHook(() => useCategoryList(), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });

    expect(result.current.problem).toMatchObject({
      title: "Request failed",
      detail: "Registry timeout",
    });
  });

  it("loads and maps category detail records", async () => {
    categoryAdminApiMock.getCategory.mockResolvedValue(
      featuredSleepCategoryResponse,
    );

    const { result } = renderHook(() => useCategoryDetail(7), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(result.current.category?.slug).toBe("featured-sleep");
    expect(result.current.category?.typeLabel).toBe("Story");
    expect(result.current.isNotFound).toBe(false);
  });

  it("marks category detail queries as not found for category_not_found responses", async () => {
    categoryAdminApiMock.getCategory.mockRejectedValue(
      makeApiClientError(
        makeProblem({
          status: 404,
          title: "Category not found",
          detail: "Category 999 was not found",
          errorCode: "category_not_found",
          path: "/api/admin/categories/999",
        }),
      ),
    );

    const { result } = renderHook(() => useCategoryDetail(999), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });

    expect(result.current.isNotFound).toBe(true);
    expect(result.current.problem?.status).toBe(404);
  });

  it("keeps generic detail failures separate from not-found handling", async () => {
    categoryAdminApiMock.getCategory.mockRejectedValue(
      new Error("Backend timeout"),
    );

    const { result } = renderHook(() => useCategoryDetail(8), {
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

  it("loads and maps persisted category localization tabs", async () => {
    categoryAdminApiMock.listLocalizations.mockResolvedValue(
      categoryLocalizationResponses,
    );

    const { result } = renderHook(() => useCategoryLocalizations(7), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(result.current.localizations).toHaveLength(2);
    expect(
      result.current.localizations.map(
        (localization) => localization.languageCode,
      ),
    ).toEqual(["tr", "en"]);
    expect(result.current.localizations).toEqual(
      expect.arrayContaining([
        expect.objectContaining({
          languageCode: "en",
          name: "Featured Sleep",
        }),
      ]),
    );
    expect(result.current.problem).toBeNull();
  });

  it("returns an empty localization list when the backend has none", async () => {
    categoryAdminApiMock.listLocalizations.mockResolvedValue([]);

    const { result } = renderHook(() => useCategoryLocalizations(7), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(result.current.localizations).toEqual([]);
    expect(result.current.problem).toBeNull();
  });

  it("surfaces localization read failures separately from empty state", async () => {
    categoryAdminApiMock.listLocalizations.mockRejectedValue(
      new Error("Localization read timeout"),
    );

    const { result } = renderHook(() => useCategoryLocalizations(7), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });

    expect(result.current.problem).toMatchObject({
      title: "Request failed",
      detail: "Localization read timeout",
    });
  });

  it("loads and maps curated category rows for one language lane", async () => {
    categoryCurationAdminApiMock.listCuratedContent.mockResolvedValue(
      categoryCurationResponses,
    );

    const { result } = renderHook(() => useCategoryCuration(7, "en"), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(result.current.items).toHaveLength(2);
    expect(result.current.items[0]).toMatchObject({
      contentId: 1,
      displayOrder: 0,
    });
    expect(result.current.problem).toBeNull();
  });

  it("surfaces curation read failures as a lane problem", async () => {
    categoryCurationAdminApiMock.listCuratedContent.mockRejectedValue(
      new Error("Curation lane timeout"),
    );

    const { result } = renderHook(() => useCategoryCuration(7, "en"), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });

    expect(result.current.problem).toMatchObject({
      title: "Request failed",
      detail: "Curation lane timeout",
    });
  });
});
