package com.tellpal.v2.content.api;

public record ContentReference(
        Long contentId,
        ContentApiType type,
        String externalKey,
        boolean active,
        Integer ageRange,
        Integer pageCount) {

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
        externalKey = externalKey.trim();
    }
}
