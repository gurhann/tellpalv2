package com.tellpal.v2.content.api;

import java.util.List;

/**
 * Admin-facing story-page view that combines stable page metadata and localized payloads.
 */
public record AdminStoryPageView(
        Long contentId,
        int pageNumber,
        int localizationCount,
        List<AdminStoryPageLocalizationView> localizations) {

    public AdminStoryPageView {
        if (contentId == null || contentId <= 0) {
            throw new IllegalArgumentException("Content ID must be positive");
        }
        if (pageNumber <= 0) {
            throw new IllegalArgumentException("Story page number must be positive");
        }
        if (localizationCount < 0) {
            throw new IllegalArgumentException("Localization count must not be negative");
        }
        localizations = localizations == null ? List.of() : List.copyOf(localizations);
    }
}
