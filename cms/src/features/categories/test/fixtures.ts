import type {
  AdminCategoryLocalizationResponse,
  AdminCategoryResponse,
} from "@/features/categories/api/category-admin";
import {
  mapAdminCategory,
  mapAdminCategoryLocalization,
  type CategoryLocalizationViewModel,
  type CategorySummaryViewModel,
} from "@/features/categories/model/category-view-model";

export const featuredSleepCategoryResponse: AdminCategoryResponse = {
  categoryId: 7,
  slug: "featured-sleep",
  type: "STORY",
  premium: false,
  active: true,
};

export const bedtimeMeditationCategoryResponse: AdminCategoryResponse = {
  categoryId: 8,
  slug: "bedtime-meditations",
  type: "MEDITATION",
  premium: true,
  active: true,
};

export const archivedLullabyCategoryResponse: AdminCategoryResponse = {
  categoryId: 9,
  slug: "quiet-nights",
  type: "LULLABY",
  premium: false,
  active: false,
};

export const categoryResponses: AdminCategoryResponse[] = [
  featuredSleepCategoryResponse,
  bedtimeMeditationCategoryResponse,
  archivedLullabyCategoryResponse,
];

export const featuredSleepCategoryViewModel = mapAdminCategory(
  featuredSleepCategoryResponse,
);
export const bedtimeMeditationCategoryViewModel = mapAdminCategory(
  bedtimeMeditationCategoryResponse,
);
export const archivedCategoryViewModel = mapAdminCategory(
  archivedLullabyCategoryResponse,
);

export const categoryViewModels: CategorySummaryViewModel[] = [
  featuredSleepCategoryViewModel,
  bedtimeMeditationCategoryViewModel,
  archivedCategoryViewModel,
];

export const featuredSleepEnglishLocalizationResponse: AdminCategoryLocalizationResponse =
  {
    categoryId: 7,
    languageCode: "en",
    name: "Featured Sleep",
    description: "Curated sleep stories for bedtime.",
    imageMediaId: 4,
    status: "PUBLISHED",
    publishedAt: "2026-03-29T11:00:00Z",
    published: true,
  };

export const featuredSleepTurkishLocalizationResponse: AdminCategoryLocalizationResponse =
  {
    categoryId: 7,
    languageCode: "tr",
    name: "One Cikan Uyku",
    description: "Uyku zamani icin secilmis hikayeler.",
    imageMediaId: null,
    status: "DRAFT",
    publishedAt: null,
    published: false,
  };

export const categoryLocalizationResponses: AdminCategoryLocalizationResponse[] =
  [
    featuredSleepEnglishLocalizationResponse,
    featuredSleepTurkishLocalizationResponse,
  ];

export const featuredSleepEnglishLocalizationViewModel: CategoryLocalizationViewModel =
  mapAdminCategoryLocalization(featuredSleepEnglishLocalizationResponse);
export const featuredSleepTurkishLocalizationViewModel: CategoryLocalizationViewModel =
  mapAdminCategoryLocalization(featuredSleepTurkishLocalizationResponse);

export const categoryLocalizationViewModels: CategoryLocalizationViewModel[] = [
  featuredSleepEnglishLocalizationViewModel,
  featuredSleepTurkishLocalizationViewModel,
];
