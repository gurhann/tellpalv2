import { useQuery } from "@tanstack/react-query";

import { categoryAdminApi } from "@/features/categories/api/category-admin";
import {
  mapAdminCategoryList,
  type CategorySummaryViewModel,
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

export function useCategoryList() {
  const query = useQuery({
    queryKey: queryKeys.categories.list(),
    queryFn: async () => {
      const response = await categoryAdminApi.listCategories();
      return mapAdminCategoryList(response);
    },
  });

  return {
    ...query,
    categories: query.data ?? ([] as CategorySummaryViewModel[]),
    problem: getApiProblem(
      query.error,
      "The category registry could not be loaded from the admin API.",
    ),
  };
}
