import { useMutation, useQueryClient } from "@tanstack/react-query";

import type { AdminCategoryResponse } from "@/features/categories/api/category-admin";
import { categoryAdminApi } from "@/features/categories/api/category-admin";
import {
  mapAdminCategory,
  type CategorySummaryViewModel,
} from "@/features/categories/model/category-view-model";
import type { CategoryFormValues } from "@/features/categories/schema/category-schema";
import { queryKeys } from "@/lib/query-keys";

type UseSaveCategoryOptions =
  | {
      mode: "create";
      onSuccess?: (category: AdminCategoryResponse) => void;
    }
  | {
      mode: "update";
      categoryId: number;
      onSuccess?: (category: AdminCategoryResponse) => void;
    };

function updateCategoryListCache(
  records: CategorySummaryViewModel[] | undefined,
  savedCategory: AdminCategoryResponse,
) {
  const nextRecord = mapAdminCategory(savedCategory);

  if (!records) {
    return [nextRecord];
  }

  const existingIndex = records.findIndex(
    (record) => record.id === savedCategory.categoryId,
  );

  if (existingIndex === -1) {
    return [nextRecord, ...records];
  }

  return records.map((record) =>
    record.id === savedCategory.categoryId ? nextRecord : record,
  );
}

export function useSaveCategory(options: UseSaveCategoryOptions) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (values: CategoryFormValues) => {
      if (options.mode === "create") {
        return categoryAdminApi.createCategory({
          type: values.type,
          slug: values.slug.trim(),
          premium: values.premium,
          active: values.active,
        });
      }

      return categoryAdminApi.updateCategory(options.categoryId, {
        type: values.type,
        slug: values.slug.trim(),
        premium: values.premium,
        active: values.active,
      });
    },
    onSuccess: async (savedCategory) => {
      const detailKey = queryKeys.categories.detail(savedCategory.categoryId);

      queryClient.setQueriesData<CategorySummaryViewModel[]>(
        { queryKey: queryKeys.categories.lists() },
        (records) => updateCategoryListCache(records, savedCategory),
      );
      queryClient.setQueryData<CategorySummaryViewModel>(
        detailKey,
        mapAdminCategory(savedCategory),
      );

      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: queryKeys.categories.lists(),
        }),
        queryClient.invalidateQueries({
          queryKey: detailKey,
        }),
      ]);

      options.onSuccess?.(savedCategory);
    },
  });
}
