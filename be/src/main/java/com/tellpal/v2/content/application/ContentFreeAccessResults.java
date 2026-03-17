package com.tellpal.v2.content.application;

import com.tellpal.v2.shared.domain.LanguageCode;

public final class ContentFreeAccessResults {

    private ContentFreeAccessResults() {
    }

    public record ContentFreeAccessRecord(
            Long freeAccessId,
            String accessKey,
            Long contentId,
            LanguageCode languageCode) {

        public ContentFreeAccessRecord {
            freeAccessId = requirePositiveId(freeAccessId, "Free-access ID must be positive");
            accessKey = requireAccessKey(accessKey);
            contentId = requirePositiveId(contentId, "Content ID must be positive");
            if (languageCode == null) {
                throw new IllegalArgumentException("Language code must not be null");
            }
        }
    }

    private static Long requirePositiveId(Long value, String message) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static String requireAccessKey(String accessKey) {
        if (accessKey == null || accessKey.isBlank()) {
            throw new IllegalArgumentException("Access key must not be blank");
        }
        return accessKey.trim();
    }
}
