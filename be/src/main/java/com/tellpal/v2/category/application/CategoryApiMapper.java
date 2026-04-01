package com.tellpal.v2.category.application;

import com.tellpal.v2.category.api.CategoryReference;
import com.tellpal.v2.category.domain.Category;

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
}
