package com.tellpal.v2.category.api;

/**
 * Stable reference to a category aggregate exposed outside the module.
 */
public record CategoryReference(
        Long categoryId,
        CategoryApiType type,
        String slug,
        boolean premium,
        boolean active) {

    public CategoryReference {
        if (categoryId == null || categoryId <= 0) {
            throw new IllegalArgumentException("Category ID must be positive");
        }
        if (type == null) {
            throw new IllegalArgumentException("Category type must not be null");
        }
        if (slug == null || slug.isBlank()) {
            throw new IllegalArgumentException("Category slug must not be blank");
        }
        slug = slug.trim();
    }
}
