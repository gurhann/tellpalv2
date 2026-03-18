package com.tellpal.v2.content.api;

import com.tellpal.v2.asset.api.ContentDeliveryAssets;
import com.tellpal.v2.shared.domain.LanguageCode;

/**
 * Public list view for a visible content localization.
 */
public record PublicContentSummary(
        Long contentId,
        ContentApiType type,
        String externalKey,
        LanguageCode languageCode,
        String title,
        String description,
        Integer ageRange,
        Integer pageCount,
        Integer durationMinutes,
        boolean free,
        ContentDeliveryAssets assets) {

    public PublicContentSummary {
        if (contentId == null || contentId <= 0) {
            throw new IllegalArgumentException("Content ID must be positive");
        }
        if (type == null) {
            throw new IllegalArgumentException("Content type must not be null");
        }
        if (externalKey == null || externalKey.isBlank()) {
            throw new IllegalArgumentException("Content external key must not be blank");
        }
        if (languageCode == null) {
            throw new IllegalArgumentException("Language code must not be null");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Content title must not be blank");
        }
        assets = assets == null ? ContentDeliveryAssets.empty() : assets;
        externalKey = externalKey.trim();
        title = title.trim();
    }
}
