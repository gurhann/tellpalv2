package com.tellpal.v2.category.application;

import com.tellpal.v2.category.api.CategoryReference;
import com.tellpal.v2.category.api.AdminCategoryLocalizationView;
import com.tellpal.v2.category.domain.Category;
import com.tellpal.v2.category.domain.CategoryLocalization;

final class CategoryApiMapper {

    private CategoryApiMapper() {
    }

    static CategoryReference toReference(Category category) {
        Long categoryId = category.getId();
        if (categoryId == null || categoryId <= 0) {
            throw new IllegalStateException("Category must be persisted before mapping to API");
        }
        return new CategoryReference(
                categoryId,
                category.getType().toContentApiType(),
                category.getSlug(),
                category.isPremium(),
                category.isActive());
    }

    static AdminCategoryLocalizationView toLocalizationView(Long categoryId, CategoryLocalization localization) {
        if (categoryId == null || categoryId <= 0) {
            throw new IllegalStateException("Category must be persisted before mapping localizations");
        }
        return new AdminCategoryLocalizationView(
                categoryId,
                localization.getLanguageCode(),
                localization.getName(),
                localization.getDescription(),
                localization.getImageMediaId(),
                localization.getStatus().name(),
                localization.getPublishedAt(),
                localization.isPublished());
    }
}
