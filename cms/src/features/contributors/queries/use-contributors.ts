import { useQuery } from "@tanstack/react-query";

import { contributorAdminApi } from "@/features/contributors/api/contributor-admin";
import {
  mapAdminContributorList,
  type ContributorViewModel,
} from "@/features/contributors/model/contributor-view-model";
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

export function useContributors(limit = 12) {
  const query = useQuery({
    queryKey: queryKeys.contributors.list({ limit }),
    queryFn: async () => {
      const response = await contributorAdminApi.listContributors(limit);
      return mapAdminContributorList(response);
    },
  });

  return {
    ...query,
    limit,
    contributors: query.data ?? ([] as ContributorViewModel[]),
    problem: getApiProblem(
      query.error,
      "The contributor registry could not be loaded from the admin API.",
    ),
  };
}
