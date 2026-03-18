package com.tellpal.v2.content.application;

import java.time.Instant;

import com.tellpal.v2.shared.domain.LanguageCode;

/**
 * Command types used by content publication application services.
 */
public final class ContentPublicationCommands {

    private ContentPublicationCommands() {
    }

    /**
     * Command for publishing a content localization.
     */
    public record PublishContentLocalizationCommand(
            Long contentId,
            LanguageCode languageCode,
            Instant publishedAt) {

        public PublishContentLocalizationCommand {
            contentId = requirePositiveId(contentId, "Content ID must be positive");
            languageCode = requireLanguageCode(languageCode);
            if (publishedAt == null) {
                throw new IllegalArgumentException("Published timestamp must not be null");
            }
        }
    }

    /**
     * Command for archiving a content localization.
     */
    public record ArchiveContentLocalizationCommand(Long contentId, LanguageCode languageCode) {

        public ArchiveContentLocalizationCommand {
            contentId = requirePositiveId(contentId, "Content ID must be positive");
            languageCode = requireLanguageCode(languageCode);
        }
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
