package com.tellpal.v2.content.api;

import java.util.List;

/**
 * Admin-facing content view that combines stable metadata and localized snapshots.
 */
public record AdminContentView(
        Long contentId,
        ContentApiType type,
        String externalKey,
        boolean active,
        Integer ageRange,
        Integer pageCount,
        List<AdminContentLocalizationView> localizations) {

    public AdminContentView {
        if (contentId == null || contentId <= 0) {
            throw new IllegalArgumentException("Content ID must be positive");
        }
        if (type == null) {
            throw new IllegalArgumentException("Content type must not be null");
        }
        if (externalKey == null || externalKey.isBlank()) {
            throw new IllegalArgumentException("Content external key must not be blank");
        }
        localizations = localizations == null ? List.of() : List.copyOf(localizations);
        externalKey = externalKey.trim();
    }
}
