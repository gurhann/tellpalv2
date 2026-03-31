package com.tellpal.v2.content.api;

import com.tellpal.v2.shared.domain.LanguageCode;

/**
 * Admin-facing localized story-page snapshot used by story-page read endpoints.
 */
public record AdminStoryPageLocalizationView(
        Long contentId,
        int pageNumber,
        LanguageCode languageCode,
        String bodyText,
        Long audioMediaId,
        Long illustrationMediaId) {

    public AdminStoryPageLocalizationView {
        if (contentId == null || contentId <= 0) {
            throw new IllegalArgumentException("Content ID must be positive");
        }
        if (pageNumber <= 0) {
            throw new IllegalArgumentException("Story page number must be positive");
        }
        if (languageCode == null) {
            throw new IllegalArgumentException("Language code must not be null");
        }
        if (illustrationMediaId == null || illustrationMediaId <= 0) {
            throw new IllegalArgumentException("Illustration media ID must be positive");
        }
    }
}
