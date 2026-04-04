import { useQuery } from "@tanstack/react-query";

import { categoryCurationAdminApi } from "@/features/categories/api/category-curation-admin";
import {
  mapAdminCategoryCurationList,
  type CategoryCurationItemViewModel,
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

export function useCategoryCuration(
  categoryId: number | null,
  languageCode: string | null,
) {
  const normalizedLanguageCode = languageCode?.trim() ?? "";
  const query = useQuery({
    queryKey: queryKeys.categories.curation(
      categoryId ?? 0,
      normalizedLanguageCode || "pending",
    ),
    enabled:
      typeof categoryId === "number" &&
      categoryId > 0 &&
      normalizedLanguageCode.length > 0,
    queryFn: async () => {
      const response = await categoryCurationAdminApi.listCuratedContent(
        categoryId as number,
        normalizedLanguageCode,
      );
      return mapAdminCategoryCurationList(response);
    },
  });

  return {
    ...query,
    items: query.data ?? ([] as CategoryCurationItemViewModel[]),
    problem: getApiProblem(
      query.error,
      "The curated content lane could not be loaded from the admin API.",
    ),
  };
}
