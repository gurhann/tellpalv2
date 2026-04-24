import type {
  AdminCategoryLocalizationResponse,
  AdminCategoryResponse,
  CategoryLocalizationStatus,
  CategoryType,
} from "@/features/categories/api/category-admin";
import type {
  AdminCategoryContentResponse,
  AdminEligibleCategoryContentResponse,
} from "@/features/categories/api/category-curation-admin";
import { mapLanguage, supportedCmsLanguageOptions } from "@/lib/languages";

const categoryTypeLabels: Record<CategoryType, string> = {
  STORY: "Story",
  AUDIO_STORY: "Audio Story",
  MEDITATION: "Meditation",
  LULLABY: "Lullaby",
};

const localizationStatusLabels: Record<CategoryLocalizationStatus, string> = {
  DRAFT: "Draft",
  PUBLISHED: "Published",
  ARCHIVED: "Archived",
};

const languageOrder = new Map(
  supportedCmsLanguageOptions.map((option, index) => [option.code, index]),
);

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
  externalKey: string;
  localizedTitle: string | null;
};

export type EligibleCategoryContentViewModel = {
  contentId: number;
  externalKey: string;
  localizedTitle: string;
  languageCode: string;
  languageLabel: string;
  publishedAt: string | null;
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

export function sortCategoryLocalizationViewModels(
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

export function mapAdminCategoryLocalizationList(
  localizations: AdminCategoryLocalizationResponse[],
): CategoryLocalizationViewModel[] {
  return sortCategoryLocalizationViewModels(
    localizations.map(mapAdminCategoryLocalization),
  );
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
    externalKey: curationItem.externalKey,
    localizedTitle: curationItem.localizedTitle,
  };
}

export function mapAdminCategoryCurationList(
  curationItems: AdminCategoryContentResponse[],
): CategoryCurationItemViewModel[] {
  return curationItems.map(mapAdminCategoryCurationItem);
}

export function mapAdminEligibleCategoryContent(
  candidate: AdminEligibleCategoryContentResponse,
): EligibleCategoryContentViewModel {
  const language = mapLanguage(candidate.languageCode);

  return {
    contentId: candidate.contentId,
    externalKey: candidate.externalKey,
    localizedTitle: candidate.localizedTitle,
    languageCode: language.code,
    languageLabel: language.label,
    publishedAt: candidate.publishedAt,
  };
}

export function mapAdminEligibleCategoryContentList(
  candidates: AdminEligibleCategoryContentResponse[],
): EligibleCategoryContentViewModel[] {
  return candidates.map(mapAdminEligibleCategoryContent);
}
