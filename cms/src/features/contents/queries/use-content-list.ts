import { useQuery } from "@tanstack/react-query";

import { contentAdminApi } from "@/features/contents/api/content-admin";
import {
  mapAdminContentReadList,
  type ContentReadViewModel,
} from "@/features/contents/model/content-view-model";
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

export function useContentList() {
  const query = useQuery({
    queryKey: queryKeys.contents.list(),
    queryFn: async () => {
      const response = await contentAdminApi.listContents();
      return mapAdminContentReadList(response);
    },
  });

  return {
    ...query,
    contents: query.data ?? ([] as ContentReadViewModel[]),
    problem: getApiProblem(
      query.error,
      "The content registry could not be loaded from the admin API.",
    ),
  };
}
