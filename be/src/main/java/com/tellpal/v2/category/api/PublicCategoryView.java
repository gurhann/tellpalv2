package com.tellpal.v2.category.api;

import com.tellpal.v2.asset.api.ResolvedAssetReference;
import com.tellpal.v2.shared.domain.LanguageCode;

public record PublicCategoryView(
        Long categoryId,
        CategoryApiType type,
        String slug,
        LanguageCode languageCode,
        String name,
        String description,
        boolean premium,
        ResolvedAssetReference image) {

    public PublicCategoryView {
        if (categoryId == null || categoryId <= 0) {
            throw new IllegalArgumentException("Category ID must be positive");
        }
        if (type == null) {
            throw new IllegalArgumentException("Category type must not be null");
        }
        if (slug == null || slug.isBlank()) {
            throw new IllegalArgumentException("Category slug must not be blank");
        }
        if (languageCode == null) {
            throw new IllegalArgumentException("Language code must not be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Category name must not be blank");
        }
        slug = slug.trim();
        name = name.trim();
    }
}
