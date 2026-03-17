package com.tellpal.v2.asset.api;

import com.tellpal.v2.shared.domain.LanguageCode;

public final class AssetProcessingCommands {

    private AssetProcessingCommands() {
    }

    public record ScheduleAssetProcessingCommand(Long contentId, LanguageCode languageCode) {

        public ScheduleAssetProcessingCommand {
            contentId = requirePositiveId(contentId, "Content ID must be positive");
            languageCode = requireLanguageCode(languageCode);
        }
    }

    public record StartAssetProcessingCommand(Long contentId, LanguageCode languageCode) {

        public StartAssetProcessingCommand {
            contentId = requirePositiveId(contentId, "Content ID must be positive");
            languageCode = requireLanguageCode(languageCode);
        }
    }

    public record RetryAssetProcessingCommand(Long contentId, LanguageCode languageCode) {

        public RetryAssetProcessingCommand {
            contentId = requirePositiveId(contentId, "Content ID must be positive");
            languageCode = requireLanguageCode(languageCode);
        }
    }

    public record RecoverExpiredAssetProcessingCommand(Long contentId, LanguageCode languageCode) {

        public RecoverExpiredAssetProcessingCommand {
            contentId = requirePositiveId(contentId, "Content ID must be positive");
            languageCode = requireLanguageCode(languageCode);
        }
    }

    public record CompleteAssetProcessingCommand(Long contentId, LanguageCode languageCode) {

        public CompleteAssetProcessingCommand {
            contentId = requirePositiveId(contentId, "Content ID must be positive");
            languageCode = requireLanguageCode(languageCode);
        }
    }

    public record FailAssetProcessingCommand(
            Long contentId,
            LanguageCode languageCode,
            String errorCode,
            String errorMessage) {

        public FailAssetProcessingCommand {
            contentId = requirePositiveId(contentId, "Content ID must be positive");
            languageCode = requireLanguageCode(languageCode);
            errorCode = normalizeOptionalText(errorCode);
            errorMessage = normalizeOptionalText(errorMessage);
            if (errorCode == null && errorMessage == null) {
                throw new IllegalArgumentException("Failure details must include an error code or message");
            }
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

    private static String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
