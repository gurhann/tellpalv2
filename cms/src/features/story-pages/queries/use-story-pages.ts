import { useQuery } from "@tanstack/react-query";

import { storyPageAdminApi } from "@/features/contents/api/story-page-admin";
import {
  mapAdminStoryPageRead,
  mapAdminStoryPageReadList,
  type StoryPageReadViewModel,
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

export function useStoryPages(contentId: number | null) {
  const query = useQuery({
    queryKey: queryKeys.contents.storyPages(contentId ?? 0),
    enabled: typeof contentId === "number" && contentId > 0,
    queryFn: async () => {
      const response = await storyPageAdminApi.listStoryPages(
        contentId as number,
      );
      return mapAdminStoryPageReadList(response);
    },
  });

  return {
    ...query,
    storyPages: query.data ?? ([] as StoryPageReadViewModel[]),
    problem: getApiProblem(
      query.error,
      "The story page collection could not be loaded from the admin API.",
    ),
  };
}

export function useStoryPage(
  contentId: number | null,
  pageNumber: number | null,
) {
  const hasValidContentId =
    typeof contentId === "number" &&
    Number.isInteger(contentId) &&
    contentId > 0;
  const hasValidPageNumber =
    typeof pageNumber === "number" &&
    Number.isInteger(pageNumber) &&
    pageNumber > 0;
  const query = useQuery({
    queryKey: queryKeys.contents.storyPage(contentId ?? 0, pageNumber ?? 0),
    enabled: hasValidContentId && hasValidPageNumber,
    queryFn: async () => {
      const response = await storyPageAdminApi.getStoryPage(
        contentId as number,
        pageNumber as number,
      );
      return mapAdminStoryPageRead(response);
    },
  });

  return {
    ...query,
    storyPage: (query.data ?? null) as StoryPageReadViewModel | null,
    problem: getApiProblem(
      query.error,
      "The requested story page could not be loaded from the admin API.",
    ),
  };
}
