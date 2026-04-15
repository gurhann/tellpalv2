package com.tellpal.v2.category.api;

import java.time.Instant;

import com.tellpal.v2.shared.domain.LanguageCode;

/**
 * Admin read model for one eligible localized content candidate.
 */
public record AdminEligibleCategoryContentView(
        Long contentId,
        String externalKey,
        String localizedTitle,
        LanguageCode languageCode,
        Instant publishedAt) {

    public AdminEligibleCategoryContentView {
        if (contentId == null || contentId <= 0) {
            throw new IllegalArgumentException("Content ID must be positive");
        }
        if (externalKey == null || externalKey.isBlank()) {
            throw new IllegalArgumentException("Content external key must not be blank");
        }
        if (localizedTitle == null || localizedTitle.isBlank()) {
            throw new IllegalArgumentException("Localized title must not be blank");
        }
        if (languageCode == null) {
            throw new IllegalArgumentException("Language code must not be null");
        }
        externalKey = externalKey.trim();
        localizedTitle = localizedTitle.trim();
    }
}
