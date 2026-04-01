import { useMutation, useQueryClient } from "@tanstack/react-query";

import { categoryAdminApi } from "@/features/categories/api/category-admin";
import {
  mapAdminCategoryLocalization,
  type CategoryLocalizationViewModel,
} from "@/features/categories/model/category-view-model";
import type { CategoryLocalizationFormValues } from "@/features/categories/schema/category-localization-schema";
import { toPublishedAtPayload } from "@/features/categories/schema/category-localization-schema";
import { supportedCmsLanguageOptions } from "@/lib/languages";
import { queryKeys } from "@/lib/query-keys";

type SaveCategoryLocalizationVariables = {
  mode: "create" | "update";
  values: CategoryLocalizationFormValues;
};

const languageOrder = new Map(
  supportedCmsLanguageOptions.map((option, index) => [option.code, index]),
);

function sortLocalizations(
  localizations: CategoryLocalizationViewModel[],
): CategoryLocalizationViewModel[] {
  return [...localizations].sort((left, right) => {
    const leftOrder =
      languageOrder.get(left.languageCode) ?? Number.MAX_SAFE_INTEGER;
    const rightOrder =
      languageOrder.get(right.languageCode) ?? Number.MAX_SAFE_INTEGER;

    if (leftOrder !== rightOrder) {
      return leftOrder - rightOrder;
    }

    return left.languageCode.localeCompare(right.languageCode);
  });
}

function upsertLocalizationList(
  currentLocalizations: CategoryLocalizationViewModel[] | undefined,
  localization: CategoryLocalizationViewModel,
) {
  const records = currentLocalizations ?? [];
  const existingIndex = records.findIndex(
    (record) => record.languageCode === localization.languageCode,
  );
  const nextLocalizations =
    existingIndex === -1
      ? [...records, localization]
      : records.map((record, index) =>
          index === existingIndex ? localization : record,
        );

  return sortLocalizations(nextLocalizations);
}

function toLocalizationPayload(values: CategoryLocalizationFormValues) {
  return {
    name: values.name.trim(),
    description: values.description,
    imageMediaId: values.imageMediaId,
    status: values.status,
    publishedAt: toPublishedAtPayload(values.publishedAt),
  };
}

export function useCategoryLocalizationActions(categoryId: number) {
  const queryClient = useQueryClient();

  return {
    saveLocalization: useMutation({
      mutationFn: async ({
        mode,
        values,
      }: SaveCategoryLocalizationVariables) => {
        const payload = toLocalizationPayload(values);

        if (mode === "create") {
          return categoryAdminApi.createLocalization(
            categoryId,
            values.languageCode,
            payload,
          );
        }

        return categoryAdminApi.updateLocalization(
          categoryId,
          values.languageCode,
          payload,
        );
      },
      onSuccess: async (response) => {
        const localization = mapAdminCategoryLocalization(response);
        const listKey = queryKeys.categories.localizations(categoryId);
        const itemKey = queryKeys.categories.localization(
          categoryId,
          localization.languageCode,
        );

        queryClient.setQueryData<CategoryLocalizationViewModel[]>(
          listKey,
          (records) => upsertLocalizationList(records, localization),
        );
        queryClient.setQueryData(itemKey, localization);

        await Promise.all([
          queryClient.invalidateQueries({
            queryKey: listKey,
          }),
          queryClient.invalidateQueries({
            queryKey: itemKey,
          }),
        ]);
      },
    }),
  };
}
