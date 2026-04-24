package com.tellpal.v2.category.api;

import com.tellpal.v2.shared.domain.LanguageCode;

/**
 * Admin read model for one localized curated content link.
 */
public record AdminCategoryContentView(
        Long categoryId,
        LanguageCode languageCode,
        Long contentId,
        int displayOrder,
        String externalKey,
        String localizedTitle) {

    public AdminCategoryContentView {
        if (categoryId == null || categoryId <= 0) {
            throw new IllegalArgumentException("Category ID must be positive");
        }
        if (languageCode == null) {
            throw new IllegalArgumentException("Language code must not be null");
        }
        if (contentId == null || contentId <= 0) {
            throw new IllegalArgumentException("Content ID must be positive");
        }
        if (displayOrder < 0) {
            throw new IllegalArgumentException("Display order must not be negative");
        }
        if (externalKey == null || externalKey.isBlank()) {
            throw new IllegalArgumentException("Content external key must not be blank");
        }
        externalKey = externalKey.trim();
    }
}
