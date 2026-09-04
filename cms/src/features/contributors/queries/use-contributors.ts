import { useQuery } from "@tanstack/react-query";

import {
  contributorAdminApi,
  type ContributorRegistryQuery,
} from "@/features/contributors/api/contributor-admin";
import {
  mapAdminContributor,
  mapAdminContributorRegistryItem,
  type ContributorViewModel,
} from "@/features/contributors/model/contributor-view-model";
import { ApiClientError } from "@/lib/http/client";
import { queryKeys } from "@/lib/query-keys";
import type { ApiProblemDetail } from "@/types/api";

function getApiProblem(error: unknown, fallbackDetail: string) {
  if (!error) return null;
  if (error instanceof ApiClientError) return error.problem;
  return {
    type: "about:blank",
    title: "Request failed",
    status: 500,
    detail: error instanceof Error ? error.message : fallbackDetail,
  } satisfies ApiProblemDetail;
}

type UseContributorsOptions = {
  limit?: number;
  search?: string;
  enabled?: boolean;
};

type UseContributorPickerOptions = {
  role: ContributorRegistryQuery["role"];
  query?: string;
  enabled?: boolean;
};

/** Legacy unpaged picker query; assignment flows still use this contract. */
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
    queryFn: async () =>
      (
        await contributorAdminApi.listContributors({
          limit,
          query: normalizedSearch || undefined,
        })
      ).map(mapAdminContributor),
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

export function useContributorRegistry(params: ContributorRegistryQuery = {}) {
  const page = params.page ?? 0;
  const size = params.size ?? 25;
  const query = useQuery({
    queryKey: queryKeys.contributors.list({
      page,
      size,
      query: params.query || null,
      role: params.role || null,
    }),
    enabled: params.enabled ?? true,
    queryFn: async () => {
      const response = await contributorAdminApi.listContributorRegistry({
        ...params,
        page,
        size,
      });
      return {
        ...response,
        items: response.items.map(mapAdminContributorRegistryItem),
      };
    },
  });
  return {
    ...query,
    page,
    size,
    totalPages: query.data?.totalPages ?? 0,
    contributors: query.data?.items ?? ([] as ContributorViewModel[]),
    totalItems: query.data?.totalItems ?? 0,
    problem: getApiProblem(
      query.error,
      "The contributor registry could not be loaded from the admin API.",
    ),
  };
}

/** Database-backed, role-scoped query used by content assignment pickers. */
export function useContributorPicker({
  role,
  query = "",
  enabled = true,
}: UseContributorPickerOptions) {
  return useContributorRegistry({ role, query, page: 0, size: 25, enabled });
}
