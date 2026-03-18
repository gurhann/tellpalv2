package com.tellpal.v2.content.api;

import com.tellpal.v2.shared.domain.LanguageCode;

/**
 * Lightweight visibility snapshot for a single content localization.
 *
 * <p>{@code visibleToMobile} reflects both publication state and processing readiness.
 */
public record ContentLocalizationReference(
        Long contentId,
        LanguageCode languageCode,
        boolean published,
        boolean visibleToMobile) {

    public ContentLocalizationReference {
        if (contentId == null || contentId <= 0) {
            throw new IllegalArgumentException("Content ID must be positive");
        }
        if (languageCode == null) {
            throw new IllegalArgumentException("Language code must not be null");
        }
    }
}
