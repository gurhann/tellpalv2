package com.tellpal.v2.category.api;

import java.time.Instant;

import com.tellpal.v2.shared.domain.LanguageCode;

/**
 * Admin-facing localized category snapshot used by category localization read endpoints.
 */
public record AdminCategoryLocalizationView(
        Long categoryId,
        LanguageCode languageCode,
        String name,
        String description,
        Long imageMediaId,
        String status,
        Instant publishedAt,
        boolean published) {

    public AdminCategoryLocalizationView {
        if (categoryId == null || categoryId <= 0) {
            throw new IllegalArgumentException("Category ID must be positive");
        }
        if (languageCode == null) {
            throw new IllegalArgumentException("Language code must not be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Category localization name must not be blank");
        }
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Category localization status must not be blank");
        }
        name = name.trim();
    }
}
