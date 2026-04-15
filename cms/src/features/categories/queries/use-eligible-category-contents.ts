import { useQuery } from "@tanstack/react-query";

import { categoryCurationAdminApi } from "@/features/categories/api/category-curation-admin";
import {
  mapAdminEligibleCategoryContentList,
  type EligibleCategoryContentViewModel,
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

type UseEligibleCategoryContentsOptions = {
  categoryId: number | null;
  languageCode: string | null;
  search: string;
  enabled: boolean;
  limit?: number;
};

export function useEligibleCategoryContents({
  categoryId,
  languageCode,
  search,
  enabled,
  limit = 20,
}: UseEligibleCategoryContentsOptions) {
  const normalizedLanguageCode = languageCode?.trim() ?? "";
  const normalizedSearch = search.trim();
  const query = useQuery({
    queryKey: queryKeys.categories.eligibleContents(
      categoryId ?? 0,
      normalizedLanguageCode || "pending",
      { limit, query: normalizedSearch || null },
    ),
    enabled:
      enabled &&
      typeof categoryId === "number" &&
      categoryId > 0 &&
      normalizedLanguageCode.length > 0,
    queryFn: async () => {
      const response = await categoryCurationAdminApi.listEligibleContents(
        categoryId as number,
        normalizedLanguageCode,
        {
          query: normalizedSearch || undefined,
          limit,
        },
      );
      return mapAdminEligibleCategoryContentList(response);
    },
  });

  return {
    ...query,
    items: query.data ?? ([] as EligibleCategoryContentViewModel[]),
    problem: getApiProblem(
      query.error,
      "Eligible curated content could not be loaded from the admin API.",
    ),
  };
}
