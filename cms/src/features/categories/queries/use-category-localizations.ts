import { useQuery } from "@tanstack/react-query";

import { categoryAdminApi } from "@/features/categories/api/category-admin";
import {
  mapAdminCategoryLocalizationList,
  type CategoryLocalizationViewModel,
} from "@/features/categories/model/category-view-model";
import { ApiClientError } from "@/lib/http/client";
import { queryKeys } from "@/lib/query-keys";
import type { ApiProblemDetail } from "@/types/api";

function getApiProblem(error: unknown, fallbackDetail: string) {
  if (!error) {
    return null;
  }

  if (error instanceof ApiClientError) {
    return error.problem;
  }

  return {
    type: "about:blank",
    title: "Request failed",
    status: 500,
    detail: error instanceof Error ? error.message : fallbackDetail,
  } satisfies ApiProblemDetail;
}

function isCategoryNotFoundProblem(problem: ApiProblemDetail | null) {
  return (
    problem?.status === 404 &&
    (problem.errorCode === "category_not_found" ||
      problem.path?.includes("/api/admin/categories/") === true)
  );
}

export function useCategoryLocalizations(categoryId: number | null) {
  const query = useQuery({
    queryKey: queryKeys.categories.localizations(categoryId ?? 0),
    enabled: typeof categoryId === "number" && categoryId > 0,
    queryFn: async () => {
      const response = await categoryAdminApi.listLocalizations(
        categoryId as number,
      );
      return mapAdminCategoryLocalizationList(response);
    },
  });

  const problem = getApiProblem(
    query.error,
    "Category localizations could not be loaded from the admin API.",
  );

  return {
    ...query,
    localizations: (query.data ?? []) as CategoryLocalizationViewModel[],
    problem,
    isNotFound: isCategoryNotFoundProblem(problem),
  };
}
