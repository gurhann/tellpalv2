package com.tellpal.v2.asset.api;

import com.tellpal.v2.shared.domain.LanguageCode;

/**
 * Command types used by the asset processing lifecycle API.
 */
public final class AssetProcessingCommands {

    private AssetProcessingCommands() {
    }

    /**
     * Requests scheduling for a content localization, including the source assets needed to build
     * derived outputs.
     */
    public record ScheduleAssetProcessingCommand(
            Long contentId,
            LanguageCode languageCode,
            AssetProcessingContentType contentType,
            String externalKey,
            Long coverSourceAssetId,
            Long audioSourceAssetId,
            Integer pageCount) {

        public ScheduleAssetProcessingCommand {
            contentId = requirePositiveId(contentId, "Content ID must be positive");
            languageCode = requireLanguageCode(languageCode);
            contentType = requireContentType(contentType);
            externalKey = requireText(externalKey, "External key must not be blank");
            coverSourceAssetId = normalizePositiveId(coverSourceAssetId, "Cover source asset ID must be positive");
            audioSourceAssetId = normalizePositiveId(audioSourceAssetId, "Audio source asset ID must be positive");
            pageCount = normalizePageCount(contentType, pageCount);
            if (requiresSingleAudioAsset(contentType) && audioSourceAssetId == null) {
                throw new IllegalArgumentException("Audio source asset ID is required for non-story processing");
            }
        }
    }

    /**
     * Requests lease acquisition for a pending processing entry.
     */
    public record StartAssetProcessingCommand(Long contentId, LanguageCode languageCode) {

        public StartAssetProcessingCommand {
            contentId = requirePositiveId(contentId, "Content ID must be positive");
            languageCode = requireLanguageCode(languageCode);
        }
    }

    /**
     * Requests a retry for a failed processing entry and refreshes its source context.
     */
    public record RetryAssetProcessingCommand(
            Long contentId,
            LanguageCode languageCode,
            AssetProcessingContentType contentType,
            String externalKey,
            Long coverSourceAssetId,
            Long audioSourceAssetId,
            Integer pageCount) {

        public RetryAssetProcessingCommand {
            contentId = requirePositiveId(contentId, "Content ID must be positive");
            languageCode = requireLanguageCode(languageCode);
            contentType = requireContentType(contentType);
            externalKey = requireText(externalKey, "External key must not be blank");
            coverSourceAssetId = normalizePositiveId(coverSourceAssetId, "Cover source asset ID must be positive");
            audioSourceAssetId = normalizePositiveId(audioSourceAssetId, "Audio source asset ID must be positive");
            pageCount = normalizePageCount(contentType, pageCount);
            if (requiresSingleAudioAsset(contentType) && audioSourceAssetId == null) {
                throw new IllegalArgumentException("Audio source asset ID is required for non-story processing");
            }
        }
    }

    /**
     * Requests recovery for an in-flight processing entry whose worker lease expired.
     */
    public record RecoverExpiredAssetProcessingCommand(Long contentId, LanguageCode languageCode) {

        public RecoverExpiredAssetProcessingCommand {
            contentId = requirePositiveId(contentId, "Content ID must be positive");
            languageCode = requireLanguageCode(languageCode);
        }
    }

    /**
     * Requests completion of an in-flight processing entry.
     */
    public record CompleteAssetProcessingCommand(Long contentId, LanguageCode languageCode) {

        public CompleteAssetProcessingCommand {
            contentId = requirePositiveId(contentId, "Content ID must be positive");
            languageCode = requireLanguageCode(languageCode);
        }
    }

    /**
     * Requests failure of an in-flight processing entry with worker-provided diagnostics.
     */
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

    private static AssetProcessingContentType requireContentType(AssetProcessingContentType contentType) {
        if (contentType == null) {
            throw new IllegalArgumentException("Processing content type must not be null");
        }
        return contentType;
    }

    private static String requireText(String value, String message) {
        String normalized = normalizeOptionalText(value);
        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private static Long normalizePositiveId(Long value, String message) {
        if (value == null) {
            return null;
        }
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static Integer normalizePageCount(AssetProcessingContentType contentType, Integer pageCount) {
        if (contentType == AssetProcessingContentType.STORY) {
            if (pageCount == null || pageCount < 0) {
                throw new IllegalArgumentException("Story processing requires a non-negative page count");
            }
            return pageCount;
        }
        if (pageCount != null) {
            throw new IllegalArgumentException("Page count is only supported for story processing");
        }
        return null;
    }

    private static boolean requiresSingleAudioAsset(AssetProcessingContentType contentType) {
        return contentType == AssetProcessingContentType.AUDIO_STORY
                || contentType == AssetProcessingContentType.MEDITATION
                || contentType == AssetProcessingContentType.LULLABY;
    }

    private static String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
