import type {
  AdminCategoryLocalizationResponse,
  AdminCategoryResponse,
} from "@/features/categories/api/category-admin";
import type { AdminCategoryContentResponse } from "@/features/categories/api/category-curation-admin";
import {
  mapAdminCategory,
  mapAdminCategoryCurationItem,
  mapAdminCategoryLocalization,
} from "@/features/categories/model/category-view-model";

describe("category view model mappers", () => {
  it("maps category summary labels", () => {
    const dto: AdminCategoryResponse = {
      categoryId: 7,
      slug: "sleep",
      type: "STORY",
      premium: false,
      active: true,
    };

    expect(mapAdminCategory(dto)).toEqual({
      id: 7,
      slug: "sleep",
      type: "STORY",
      typeLabel: "Story",
      premium: false,
      active: true,
    });
  });

  it("maps localized category data and curated content rows", () => {
    const localizationDto: AdminCategoryLocalizationResponse = {
      categoryId: 7,
      languageCode: "EN",
      name: "Sleep",
      description: "Stories to wind down.",
      imageMediaId: 12,
      status: "PUBLISHED",
      publishedAt: "2026-03-29T11:00:00Z",
      published: true,
    };

    const curationDto: AdminCategoryContentResponse = {
      categoryId: 7,
      languageCode: "tr",
      contentId: 99,
      displayOrder: 4,
    };

    expect(mapAdminCategoryLocalization(localizationDto)).toEqual({
      categoryId: 7,
      languageCode: "en",
      languageLabel: "English",
      name: "Sleep",
      description: "Stories to wind down.",
      imageAssetId: 12,
      status: "PUBLISHED",
      statusLabel: "Published",
      publishedAt: "2026-03-29T11:00:00Z",
      isPublished: true,
      hasImage: true,
    });

    expect(mapAdminCategoryCurationItem(curationDto)).toEqual({
      categoryId: 7,
      languageCode: "tr",
      languageLabel: "Turkish",
      contentId: 99,
      displayOrder: 4,
    });
  });
});
