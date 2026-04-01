import { useQuery, useQueryClient } from "@tanstack/react-query";

import type { CategoryLocalizationViewModel } from "@/features/categories/model/category-view-model";
import { queryKeys } from "@/lib/query-keys";

export function useCategoryLocalizations(categoryId: number | null) {
  const queryClient = useQueryClient();
  const listKey = queryKeys.categories.localizations(categoryId ?? 0);

  const query = useQuery({
    queryKey: listKey,
    enabled: typeof categoryId === "number" && categoryId > 0,
    initialData: () =>
      (queryClient.getQueryData(listKey) ??
        []) as CategoryLocalizationViewModel[],
    queryFn: async () =>
      (queryClient.getQueryData(listKey) ??
        []) as CategoryLocalizationViewModel[],
    staleTime: Number.POSITIVE_INFINITY,
  });

  return {
    ...query,
    localizations: (query.data ?? []) as CategoryLocalizationViewModel[],
    isSessionBacked: true,
  };
}
