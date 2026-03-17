package com.tellpal.v2.content.application;

import com.tellpal.v2.shared.domain.LanguageCode;

public final class ContentFreeAccessCommands {

    private ContentFreeAccessCommands() {
    }

    public record GrantContentFreeAccessCommand(String accessKey, Long contentId, LanguageCode languageCode) {

        public GrantContentFreeAccessCommand {
            accessKey = requireAccessKey(accessKey);
            contentId = requirePositiveId(contentId, "Content ID must be positive");
            languageCode = requireLanguageCode(languageCode);
        }
    }

    public record RevokeContentFreeAccessCommand(String accessKey, Long contentId, LanguageCode languageCode) {

        public RevokeContentFreeAccessCommand {
            accessKey = requireAccessKey(accessKey);
            contentId = requirePositiveId(contentId, "Content ID must be positive");
            languageCode = requireLanguageCode(languageCode);
        }
    }

    private static String requireAccessKey(String accessKey) {
        if (accessKey == null || accessKey.isBlank()) {
            throw new IllegalArgumentException("Access key must not be blank");
        }
        return accessKey.trim();
    }

    private static Long requirePositiveId(Long value, String message) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static LanguageCode requireLanguageCode(LanguageCode languageCode) {
        if (languageCode == null) {
            throw new IllegalArgumentException("Language code must not be null");
        }
        return languageCode;
    }
}
