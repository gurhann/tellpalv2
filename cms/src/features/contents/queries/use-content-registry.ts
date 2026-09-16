import { useQueries, useQuery } from "@tanstack/react-query";

import {
  contentAdminApi,
  type ContentRegistryQuery,
  type ContentType,
} from "@/features/contents/api/content-admin";
import { ApiClientError } from "@/lib/http/client";
import { queryKeys } from "@/lib/query-keys";
import type { ApiProblemDetail } from "@/types/api";

export function useContentRegistry(params: ContentRegistryQuery) {
  const query = useQuery({
    queryKey: queryKeys.contents.registry(params),
    queryFn: () => contentAdminApi.listRegistry(params),
  });
  const problem: ApiProblemDetail | null =
    query.error instanceof ApiClientError
      ? query.error.problem
      : query.error
        ? {
            type: "about:blank",
            title: "Request failed",
            status: 500,
            detail: "The content registry could not be loaded.",
          }
        : null;
  return { ...query, registry: query.data, problem };
}

const contentTypes: ContentType[] = ["STORY", "MEDITATION", "LULLABY"];

export function useContentRegistryCounts(
  params: Omit<ContentRegistryQuery, "type" | "page" | "size">,
) {
  const queries = useQueries({
    queries: contentTypes.map((type) => ({
      queryKey: queryKeys.contents.registry({
        ...params,
        type,
        page: 0,
        size: 1,
      }),
      queryFn: () =>
        contentAdminApi.listRegistry({
          ...params,
          type,
          page: 0,
          size: 1,
        }),
    })),
  });

  return Object.fromEntries(
    contentTypes.map((type, index) => [
      type,
      queries[index]?.data?.totalItems ?? null,
    ]),
  ) as Record<ContentType, number | null>;
}
