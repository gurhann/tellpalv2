import { useQuery } from "@tanstack/react-query";

import { contributorAdminApi } from "@/features/contributors/api/contributor-admin";
import { mapAdminContentContributor } from "@/features/contributors/model/contributor-view-model";
import type { ContentContributorViewModel } from "@/features/contributors/model/contributor-view-model";
import { queryKeys } from "@/lib/query-keys";
import { ApiClientError } from "@/lib/http/client";
import type { ApiProblemDetail } from "@/types/api";

function sortAssignments(assignments: ContentContributorViewModel[]) {
  return [...assignments].sort((left, right) => {
    const leftLanguageCode = left.languageCode ?? "";
    const rightLanguageCode = right.languageCode ?? "";

    if (left.languageCode === null && right.languageCode !== null) {
      return -1;
    }

    if (left.languageCode !== null && right.languageCode === null) {
      return 1;
    }

    if (left.languageCode !== right.languageCode) {
      return leftLanguageCode.localeCompare(rightLanguageCode);
    }

    if (left.roleLabel !== right.roleLabel) {
      return left.roleLabel.localeCompare(right.roleLabel);
    }

    if (left.sortOrder !== right.sortOrder) {
      return left.sortOrder - right.sortOrder;
    }

    return left.displayName.localeCompare(right.displayName);
  });
}

export function useContentContributorAssignments(contentId: number | null) {
  const query = useQuery({
    enabled: contentId !== null,
    queryKey: contentId
      ? queryKeys.contributors.assignments(contentId)
      : queryKeys.contributors.assignments(-1),
    queryFn: async () => {
      if (contentId === null) {
        return [] as ContentContributorViewModel[];
      }

      const response =
        await contributorAdminApi.listContentContributors(contentId);
      return response.map(mapAdminContentContributor);
    },
  });

  const problem =
    query.error instanceof ApiClientError
      ? query.error.problem
      : query.error
        ? ({
            type: "about:blank",
            title: "Request failed",
            status: 500,
            detail:
              query.error instanceof Error
                ? query.error.message
                : "The content contributor assignments could not be loaded.",
          } satisfies ApiProblemDetail)
        : null;

  return {
    ...query,
    assignments: sortAssignments(query.data ?? []),
    problem,
  };
}
