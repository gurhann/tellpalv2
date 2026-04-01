import { useQuery, useQueryClient } from "@tanstack/react-query";

import type { ContentContributorViewModel } from "@/features/contributors/model/contributor-view-model";
import { queryKeys } from "@/lib/query-keys";

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
  const queryClient = useQueryClient();

  const query = useQuery({
    enabled: contentId !== null,
    staleTime: Infinity,
    queryKey: contentId
      ? queryKeys.contributors.assignments(contentId)
      : queryKeys.contributors.assignments(-1),
    queryFn: async () => {
      if (contentId === null) {
        return [] as ContentContributorViewModel[];
      }

      return (
        queryClient.getQueryData<ContentContributorViewModel[]>(
          queryKeys.contributors.assignments(contentId),
        ) ?? []
      );
    },
    initialData: [] as ContentContributorViewModel[],
  });

  return {
    ...query,
    assignments: sortAssignments(query.data ?? []),
  };
}
