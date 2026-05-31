import { useQuery } from "@tanstack/react-query";

import { contributorAdminApi } from "@/features/contributors/api/contributor-admin";
import {
  mapAdminContributorList,
  type ContributorViewModel,
} from "@/features/contributors/model/contributor-view-model";
import { ApiClientError } from "@/lib/http/client";
import { queryKeys } from "@/lib/query-keys";
import type { ApiProblemDetail } from "@/types/api";

type UseContributorsOptions = {
  limit?: number;
  search?: string;
  enabled?: boolean;
};

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

export function useContributors({
  limit = 12,
  search = "",
  enabled = true,
}: UseContributorsOptions = {}) {
  const normalizedSearch = search.trim();
  const query = useQuery({
    queryKey: queryKeys.contributors.list({
      limit,
      query: normalizedSearch || null,
    }),
    enabled,
    queryFn: async () => {
      const response = await contributorAdminApi.listContributors({
        limit,
        query: normalizedSearch || undefined,
      });
      return mapAdminContributorList(response);
    },
  });

  return {
    ...query,
    limit,
    search: normalizedSearch,
    contributors: query.data ?? ([] as ContributorViewModel[]),
    problem: getApiProblem(
      query.error,
      "The contributor registry could not be loaded from the admin API.",
    ),
  };
}
