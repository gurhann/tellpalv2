package com.tellpal.v2.content.api;

import java.util.Set;

import com.tellpal.v2.shared.domain.LanguageCode;

/**
 * Effective free-access set resolved for one language and access key.
 */
public record ResolvedContentFreeAccessSet(
        LanguageCode languageCode,
        String accessKey,
        Set<Long> contentIds) {

    public ResolvedContentFreeAccessSet {
        if (languageCode == null) {
            throw new IllegalArgumentException("Language code must not be null");
        }
        if (accessKey == null || accessKey.isBlank()) {
            throw new IllegalArgumentException("Access key must not be blank");
        }
        contentIds = contentIds == null ? Set.of() : Set.copyOf(contentIds);
    }

    /**
     * Checks whether the resolved access set contains the given content ID.
     */
    public boolean contains(Long contentId) {
        if (contentId == null || contentId <= 0) {
            throw new IllegalArgumentException("Content ID must be positive");
        }
        return contentIds.contains(contentId);
    }
}
