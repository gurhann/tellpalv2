package com.tellpal.v2.content.application;

import com.tellpal.v2.shared.domain.LanguageCode;

/**
 * Command types used by content free-access management services.
 */
public final class ContentFreeAccessCommands {

    private ContentFreeAccessCommands() {
    }

    /**
     * Command for granting localized free access to a content item.
     */
    public record GrantContentFreeAccessCommand(String accessKey, Long contentId, LanguageCode languageCode) {

        public GrantContentFreeAccessCommand {
            accessKey = requireAccessKey(accessKey);
            contentId = requirePositiveId(contentId, "Content ID must be positive");
            languageCode = requireLanguageCode(languageCode);
        }
    }

    /**
     * Command for revoking localized free access from a content item.
     */
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
