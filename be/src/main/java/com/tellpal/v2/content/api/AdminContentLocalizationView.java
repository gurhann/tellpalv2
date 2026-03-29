package com.tellpal.v2.content.api;

import java.time.Instant;

import com.tellpal.v2.shared.domain.LanguageCode;

/**
 * Admin-facing localized content snapshot used by content read endpoints.
 */
public record AdminContentLocalizationView(
        Long contentId,
        LanguageCode languageCode,
        String title,
        String description,
        String bodyText,
        Long coverMediaId,
        Long audioMediaId,
        Integer durationMinutes,
        String status,
        String processingStatus,
        Instant publishedAt,
        boolean visibleToMobile) {

    public AdminContentLocalizationView {
        if (contentId == null || contentId <= 0) {
            throw new IllegalArgumentException("Content ID must be positive");
        }
        if (languageCode == null) {
            throw new IllegalArgumentException("Language code must not be null");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Content localization title must not be blank");
        }
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Localization status must not be blank");
        }
        if (processingStatus == null || processingStatus.isBlank()) {
            throw new IllegalArgumentException("Processing status must not be blank");
        }
        title = title.trim();
    }
}
