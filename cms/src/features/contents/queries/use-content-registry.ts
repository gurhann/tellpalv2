import { useQuery } from "@tanstack/react-query";

import {
  contentAdminApi,
  type ContentRegistryQuery,
} from "@/features/contents/api/content-admin";
import { ApiClientError } from "@/lib/http/client";
import { queryKeys } from "@/lib/query-keys";
import type { ApiProblemDetail } from "@/types/api";

export function useContentRegistry(params: ContentRegistryQuery) {
  const query = useQuery({
    queryKey: queryKeys.contents.list(params),
    queryFn: () => contentAdminApi.listRegistry(params),
  });
  const problem: ApiProblemDetail | null = query.error instanceof ApiClientError
    ? query.error.problem
    : query.error
      ? { type: "about:blank", title: "Request failed", status: 500, detail: "The content registry could not be loaded." }
      : null;
  return { ...query, registry: query.data, problem };
}
