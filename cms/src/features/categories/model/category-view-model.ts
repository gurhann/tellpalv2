import type {
  AdminCategoryLocalizationResponse,
  AdminCategoryResponse,
  CategoryLocalizationStatus,
  CategoryType,
} from "@/features/categories/api/category-admin";
import type { AdminCategoryContentResponse } from "@/features/categories/api/category-curation-admin";
import { mapLanguage } from "@/lib/languages";

const categoryTypeLabels: Record<CategoryType, string> = {
  CONTENT: "Content",
  PARENT_GUIDANCE: "Parent Guidance",
};

const localizationStatusLabels: Record<CategoryLocalizationStatus, string> = {
  DRAFT: "Draft",
  PUBLISHED: "Published",
  ARCHIVED: "Archived",
};

export type CategorySummaryViewModel = {
  id: number;
  slug: string;
  type: CategoryType;
  typeLabel: string;
  premium: boolean;
  active: boolean;
};

export type CategoryLocalizationViewModel = {
  categoryId: number;
  languageCode: string;
  languageLabel: string;
  name: string;
  description: string | null;
  imageAssetId: number | null;
  status: CategoryLocalizationStatus;
  statusLabel: string;
  publishedAt: string | null;
  isPublished: boolean;
  hasImage: boolean;
};

export type CategoryCurationItemViewModel = {
  categoryId: number;
  languageCode: string;
  languageLabel: string;
  contentId: number;
  displayOrder: number;
};

export function mapAdminCategory(
  category: AdminCategoryResponse,
): CategorySummaryViewModel {
  return {
    id: category.categoryId,
    slug: category.slug,
    type: category.type,
    typeLabel: categoryTypeLabels[category.type],
    premium: category.premium,
    active: category.active,
  };
}

export function mapAdminCategoryList(
  categories: AdminCategoryResponse[],
): CategorySummaryViewModel[] {
  return categories.map(mapAdminCategory);
}

export function mapAdminCategoryLocalization(
  localization: AdminCategoryLocalizationResponse,
): CategoryLocalizationViewModel {
  const language = mapLanguage(localization.languageCode);

  return {
    categoryId: localization.categoryId,
    languageCode: language.code,
    languageLabel: language.label,
    name: localization.name,
    description: localization.description,
    imageAssetId: localization.imageMediaId,
    status: localization.status,
    statusLabel: localizationStatusLabels[localization.status],
    publishedAt: localization.publishedAt,
    isPublished: localization.published,
    hasImage: localization.imageMediaId !== null,
  };
}

export function mapAdminCategoryCurationItem(
  curationItem: AdminCategoryContentResponse,
): CategoryCurationItemViewModel {
  const language = mapLanguage(curationItem.languageCode);

  return {
    categoryId: curationItem.categoryId,
    languageCode: language.code,
    languageLabel: language.label,
    contentId: curationItem.contentId,
    displayOrder: curationItem.displayOrder,
  };
}

export function mapAdminCategoryCurationList(
  curationItems: AdminCategoryContentResponse[],
): CategoryCurationItemViewModel[] {
  return curationItems.map(mapAdminCategoryCurationItem);
}
