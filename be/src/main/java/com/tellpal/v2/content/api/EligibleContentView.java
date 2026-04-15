package com.tellpal.v2.content.api;

import java.time.Instant;

import com.tellpal.v2.shared.domain.LanguageCode;

/**
 * Lightweight localized content candidate used by admin pickers.
 */
public record EligibleContentView(
        Long contentId,
        String externalKey,
        String localizedTitle,
        LanguageCode languageCode,
        Instant publishedAt) {

    public EligibleContentView {
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
