package com.tellpal.v2.content.api;

import com.tellpal.v2.shared.domain.LanguageCode;

/**
 * Localized identity snapshot exposed to other modules.
 */
public record LocalizedContentIdentityReference(
        Long contentId,
        String externalKey,
        String localizedTitle,
        LanguageCode languageCode) {

    public LocalizedContentIdentityReference {
        if (contentId == null || contentId <= 0) {
            throw new IllegalArgumentException("Content ID must be positive");
        }
        if (externalKey == null || externalKey.isBlank()) {
            throw new IllegalArgumentException("Content external key must not be blank");
        }
        if (languageCode == null) {
            throw new IllegalArgumentException("Language code must not be null");
        }
        externalKey = externalKey.trim();
    }
}
