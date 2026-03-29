import { useQuery } from "@tanstack/react-query";

import { contentAdminApi } from "@/features/contents/api/content-admin";
import {
  mapAdminContentRead,
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

function isContentNotFoundProblem(problem: ApiProblemDetail | null) {
  return (
    problem?.status === 404 &&
    (problem.errorCode === "content_not_found" ||
      problem.path?.includes("/api/admin/contents/") === true)
  );
}

export function useContentDetail(contentId: number | null) {
  const query = useQuery({
    queryKey: queryKeys.contents.detail(contentId ?? 0),
    enabled: typeof contentId === "number" && contentId > 0,
    queryFn: async () => {
      const response = await contentAdminApi.getContent(contentId as number);
      return mapAdminContentRead(response);
    },
  });

  const problem = getApiProblem(
    query.error,
    "The selected content record could not be loaded from the admin API.",
  );

  return {
    ...query,
    content: (query.data ?? null) as ContentReadViewModel | null,
    problem,
    isNotFound: isContentNotFoundProblem(problem),
  };
}
