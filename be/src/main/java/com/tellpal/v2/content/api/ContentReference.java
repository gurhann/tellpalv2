package com.tellpal.v2.content.api;

/**
 * Stable reference to a content aggregate exposed outside the module.
 */
public record ContentReference(
        Long contentId,
        ContentApiType type,
        String externalKey,
        boolean active,
        Integer ageRange,
        Integer pageCount,
        Long textlessCoverMediaId) {

    public ContentReference(
            Long contentId,
            ContentApiType type,
            String externalKey,
            boolean active,
            Integer ageRange,
            Integer pageCount) {
        this(contentId, type, externalKey, active, ageRange, pageCount, null);
    }

    public ContentReference {
        if (contentId == null || contentId <= 0) {
            throw new IllegalArgumentException("Content ID must be positive");
        }
        if (type == null) {
            throw new IllegalArgumentException("Content type must not be null");
        }
        if (externalKey == null || externalKey.isBlank()) {
            throw new IllegalArgumentException("Content external key must not be blank");
        }
        if (textlessCoverMediaId != null && textlessCoverMediaId <= 0) {
            throw new IllegalArgumentException("Textless cover media ID must be positive");
        }
        externalKey = externalKey.trim();
    }
}
