import type { AdminCategoryResponse } from "@/features/categories/api/category-admin";
import {
  mapAdminCategory,
  type CategorySummaryViewModel,
} from "@/features/categories/model/category-view-model";

export const featuredSleepCategoryResponse: AdminCategoryResponse = {
  categoryId: 7,
  slug: "featured-sleep",
  type: "CONTENT",
  premium: false,
  active: true,
};

export const parentGuidanceCategoryResponse: AdminCategoryResponse = {
  categoryId: 8,
  slug: "bedtime-guidance",
  type: "PARENT_GUIDANCE",
  premium: true,
  active: true,
};

export const archivedCategoryResponse: AdminCategoryResponse = {
  categoryId: 9,
  slug: "quiet-nights",
  type: "CONTENT",
  premium: false,
  active: false,
};

export const categoryResponses: AdminCategoryResponse[] = [
  featuredSleepCategoryResponse,
  parentGuidanceCategoryResponse,
  archivedCategoryResponse,
];

export const featuredSleepCategoryViewModel = mapAdminCategory(
  featuredSleepCategoryResponse,
);
export const parentGuidanceCategoryViewModel = mapAdminCategory(
  parentGuidanceCategoryResponse,
);
export const archivedCategoryViewModel = mapAdminCategory(
  archivedCategoryResponse,
);

export const categoryViewModels: CategorySummaryViewModel[] = [
  featuredSleepCategoryViewModel,
  parentGuidanceCategoryViewModel,
  archivedCategoryViewModel,
];
