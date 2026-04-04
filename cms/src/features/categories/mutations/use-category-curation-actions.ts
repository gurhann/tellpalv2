import { useMutation, useQueryClient } from "@tanstack/react-query";

import { categoryCurationAdminApi } from "@/features/categories/api/category-curation-admin";
import {
  mapAdminCategoryCurationItem,
  type CategoryCurationItemViewModel,
} from "@/features/categories/model/category-view-model";
import { queryKeys } from "@/lib/query-keys";

type UseCategoryCurationActionsOptions = {
  categoryId: number;
  languageCode: string;
};

type UpdateCuratedContentOrderVariables = {
  contentId: number;
  displayOrder: number;
};

function sortCurationItems(
  items: CategoryCurationItemViewModel[],
): CategoryCurationItemViewModel[] {
  return [...items].sort((left, right) => {
    if (left.displayOrder !== right.displayOrder) {
      return left.displayOrder - right.displayOrder;
    }

    return left.contentId - right.contentId;
  });
}

function upsertCurationItem(
  currentItems: CategoryCurationItemViewModel[] | undefined,
  nextItem: CategoryCurationItemViewModel,
) {
  const records = currentItems ?? [];
  const existingIndex = records.findIndex(
    (record) => record.contentId === nextItem.contentId,
  );
  const nextRecords =
    existingIndex === -1
      ? [...records, nextItem]
      : records.map((record, index) =>
          index === existingIndex ? nextItem : record,
        );

  return sortCurationItems(nextRecords);
}

export function useCategoryCurationActions({
  categoryId,
  languageCode,
}: UseCategoryCurationActionsOptions) {
  const queryClient = useQueryClient();
  const curationKey = queryKeys.categories.curation(categoryId, languageCode);

  return {
    addCuratedContent: useMutation({
      mutationFn: (input: { contentId: number; displayOrder: number }) =>
        categoryCurationAdminApi.addCuratedContent(
          categoryId,
          languageCode,
          input,
        ),
      onSuccess: async (response) => {
        const curationItem = mapAdminCategoryCurationItem(response);

        queryClient.setQueryData<CategoryCurationItemViewModel[]>(
          curationKey,
          (records) => upsertCurationItem(records, curationItem),
        );

        await queryClient.invalidateQueries({
          queryKey: curationKey,
        });
      },
    }),
    updateCuratedContentOrder: useMutation({
      mutationFn: ({
        contentId,
        displayOrder,
      }: UpdateCuratedContentOrderVariables) =>
        categoryCurationAdminApi.updateCuratedContentOrder(
          categoryId,
          languageCode,
          contentId,
          {
            displayOrder,
          },
        ),
      onSuccess: async (response) => {
        const curationItem = mapAdminCategoryCurationItem(response);

        queryClient.setQueryData<CategoryCurationItemViewModel[]>(
          curationKey,
          (records) => upsertCurationItem(records, curationItem),
        );

        await queryClient.invalidateQueries({
          queryKey: curationKey,
        });
      },
    }),
  };
}
