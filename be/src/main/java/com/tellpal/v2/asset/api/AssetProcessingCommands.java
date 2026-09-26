package com.tellpal.v2.asset.api;

import com.tellpal.v2.shared.domain.LanguageCode;
import java.util.List;

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
            AssetProcessingTarget target,
            AssetProcessingKind kind,
            AssetProcessingContentType contentType,
            String externalKey,
            Long coverSourceAssetId,
            Long audioSourceAssetId,
            Integer pageCount,
            List<AssetProcessingPageSource> pageSources) {

        public ScheduleAssetProcessingCommand(AssetProcessingTarget target, AssetProcessingKind kind,
                AssetProcessingContentType contentType, String externalKey, Long coverSourceAssetId,
                Long audioSourceAssetId, Integer pageCount) {
            this(target, kind, contentType, externalKey, coverSourceAssetId, audioSourceAssetId, pageCount, List.of());
        }

        public ScheduleAssetProcessingCommand(
                AssetProcessingTarget target,
                AssetProcessingContentType contentType,
                String externalKey,
                Long coverSourceAssetId,
                Long audioSourceAssetId,
                Integer pageCount) {
            this(target, AssetProcessingKind.DELIVERY, contentType, externalKey, coverSourceAssetId,
                    audioSourceAssetId, pageCount, List.of());
        }

        public ScheduleAssetProcessingCommand(
                Long contentId,
                LanguageCode languageCode,
                AssetProcessingContentType contentType,
                String externalKey,
                Long coverSourceAssetId,
                Long audioSourceAssetId,
                Integer pageCount) {
            this(AssetProcessingTarget.localization(contentId, languageCode), AssetProcessingKind.DELIVERY, contentType, externalKey,
                    coverSourceAssetId, audioSourceAssetId, pageCount, List.of());
        }

        public ScheduleAssetProcessingCommand {
            target = requireTarget(target);
            if (kind == null) {
                throw new IllegalArgumentException("Processing kind must not be null");
            }
            contentType = requireContentType(contentType);
            validateKindTarget(kind, target, contentType);
            externalKey = requireText(externalKey, "External key must not be blank");
            coverSourceAssetId = normalizePositiveId(coverSourceAssetId, "Cover source asset ID must be positive");
            audioSourceAssetId = normalizePositiveId(audioSourceAssetId, "Audio source asset ID must be positive");
            pageCount = normalizePageCount(kind, contentType, pageCount);
            pageSources = pageSources == null ? List.of() : List.copyOf(pageSources);
            validatePageSources(kind, target, contentType, pageSources);
            if (kind == AssetProcessingKind.STORY_NARRATION && audioSourceAssetId == null) {
                throw new IllegalArgumentException("Narration audio source asset ID is required");
            }
            if (requiresSingleAudioAsset(contentType) && audioSourceAssetId == null) {
                throw new IllegalArgumentException("Audio source asset ID is required for non-story processing");
            }
        }

        public Long contentId() { return target.contentId(); }

        public LanguageCode languageCode() { return target.languageCode(); }
    }

    /**
     * Requests lease acquisition for a pending processing entry.
     */
    public record StartAssetProcessingCommand(AssetProcessingTarget target, AssetProcessingKind kind) {

        public StartAssetProcessingCommand(AssetProcessingTarget target) {
            this(target, AssetProcessingKind.DELIVERY);
        }

        public StartAssetProcessingCommand(Long contentId, LanguageCode languageCode) {
            this(AssetProcessingTarget.localization(contentId, languageCode), AssetProcessingKind.DELIVERY);
        }

        public StartAssetProcessingCommand {
            target = requireTarget(target);
            if (kind == null) throw new IllegalArgumentException("Processing kind must not be null");
        }

        public Long contentId() { return target.contentId(); }

        public LanguageCode languageCode() { return target.languageCode(); }
    }

    /**
     * Requests a retry for a failed processing entry and refreshes its source context.
     */
    public record RetryAssetProcessingCommand(
            AssetProcessingTarget target,
            AssetProcessingKind kind,
            AssetProcessingContentType contentType,
            String externalKey,
            Long coverSourceAssetId,
            Long audioSourceAssetId,
            Integer pageCount,
            List<AssetProcessingPageSource> pageSources) {

        public RetryAssetProcessingCommand(AssetProcessingTarget target, AssetProcessingKind kind,
                AssetProcessingContentType contentType, String externalKey, Long coverSourceAssetId,
                Long audioSourceAssetId, Integer pageCount) {
            this(target, kind, contentType, externalKey, coverSourceAssetId, audioSourceAssetId, pageCount, List.of());
        }

        public RetryAssetProcessingCommand(
                Long contentId,
                LanguageCode languageCode,
                AssetProcessingContentType contentType,
                String externalKey,
                Long coverSourceAssetId,
                Long audioSourceAssetId,
                Integer pageCount) {
            this(AssetProcessingTarget.localization(contentId, languageCode), AssetProcessingKind.DELIVERY, contentType, externalKey,
                    coverSourceAssetId, audioSourceAssetId, pageCount, List.of());
        }

        public RetryAssetProcessingCommand(AssetProcessingTarget target, AssetProcessingContentType contentType,
                String externalKey, Long coverSourceAssetId, Long audioSourceAssetId, Integer pageCount) {
            this(target, AssetProcessingKind.DELIVERY, contentType, externalKey, coverSourceAssetId, audioSourceAssetId, pageCount, List.of());
        }

        public RetryAssetProcessingCommand {
            target = requireTarget(target);
            if (kind == null) throw new IllegalArgumentException("Processing kind must not be null");
            contentType = requireContentType(contentType);
            validateKindTarget(kind, target, contentType);
            externalKey = requireText(externalKey, "External key must not be blank");
            coverSourceAssetId = normalizePositiveId(coverSourceAssetId, "Cover source asset ID must be positive");
            audioSourceAssetId = normalizePositiveId(audioSourceAssetId, "Audio source asset ID must be positive");
            pageCount = normalizePageCount(kind, contentType, pageCount);
            pageSources = pageSources == null ? List.of() : List.copyOf(pageSources);
            if (!(kind == AssetProcessingKind.STORY_PAGE_MEDIA && pageSources.isEmpty())) {
                validatePageSources(kind, target, contentType, pageSources);
            }
            if (kind == AssetProcessingKind.STORY_NARRATION && audioSourceAssetId == null) {
                throw new IllegalArgumentException("Narration audio source asset ID is required");
            }
            if (requiresSingleAudioAsset(contentType) && audioSourceAssetId == null) {
                throw new IllegalArgumentException("Audio source asset ID is required for non-story processing");
            }
        }

        public Long contentId() { return target.contentId(); }

        public LanguageCode languageCode() { return target.languageCode(); }
    }

    /**
     * Requests recovery for an in-flight processing entry whose worker lease expired.
     */
    public record RecoverExpiredAssetProcessingCommand(AssetProcessingTarget target, AssetProcessingKind kind) {

        public RecoverExpiredAssetProcessingCommand(AssetProcessingTarget target) {
            this(target, AssetProcessingKind.DELIVERY);
        }

        public RecoverExpiredAssetProcessingCommand(Long contentId, LanguageCode languageCode) {
            this(AssetProcessingTarget.localization(contentId, languageCode), AssetProcessingKind.DELIVERY);
        }

        public RecoverExpiredAssetProcessingCommand {
            target = requireTarget(target);
            if (kind == null) throw new IllegalArgumentException("Processing kind must not be null");
        }

        public Long contentId() { return target.contentId(); }

        public LanguageCode languageCode() { return target.languageCode(); }
    }

    /**
     * Requests completion of an in-flight processing entry.
     */
    public record CompleteAssetProcessingCommand(AssetProcessingTarget target, AssetProcessingKind kind) {

        public CompleteAssetProcessingCommand(AssetProcessingTarget target) {
            this(target, AssetProcessingKind.DELIVERY);
        }

        public CompleteAssetProcessingCommand(Long contentId, LanguageCode languageCode) {
            this(AssetProcessingTarget.localization(contentId, languageCode), AssetProcessingKind.DELIVERY);
        }

        public CompleteAssetProcessingCommand {
            target = requireTarget(target);
            if (kind == null) throw new IllegalArgumentException("Processing kind must not be null");
        }

        public Long contentId() { return target.contentId(); }

        public LanguageCode languageCode() { return target.languageCode(); }
    }

    /**
     * Requests failure of an in-flight processing entry with worker-provided diagnostics.
     */
    public record FailAssetProcessingCommand(
            AssetProcessingTarget target,
            AssetProcessingKind kind,
            String errorCode,
            String errorMessage) {

        public FailAssetProcessingCommand(
                Long contentId,
                LanguageCode languageCode,
                String errorCode,
                String errorMessage) {
            this(AssetProcessingTarget.localization(contentId, languageCode), AssetProcessingKind.DELIVERY, errorCode, errorMessage);
        }

        public FailAssetProcessingCommand(AssetProcessingTarget target, String errorCode, String errorMessage) {
            this(target, AssetProcessingKind.DELIVERY, errorCode, errorMessage);
        }

        public FailAssetProcessingCommand {
            target = requireTarget(target);
            if (kind == null) throw new IllegalArgumentException("Processing kind must not be null");
            errorCode = normalizeOptionalText(errorCode);
            errorMessage = normalizeOptionalText(errorMessage);
            if (errorCode == null && errorMessage == null) {
                throw new IllegalArgumentException("Failure details must include an error code or message");
            }
        }

        public Long contentId() { return target.contentId(); }

        public LanguageCode languageCode() { return target.languageCode(); }
    }

    private static AssetProcessingTarget requireTarget(AssetProcessingTarget target) {
        if (target == null) {
            throw new IllegalArgumentException("Asset processing target must not be null");
        }
        return target;
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

    private static Integer normalizePageCount(AssetProcessingKind kind,
            AssetProcessingContentType contentType, Integer pageCount) {
        if (kind == AssetProcessingKind.STORY_NARRATION) {
            if (pageCount != null && pageCount != 0) {
                throw new IllegalArgumentException("Page count is not supported for story narration processing");
            }
            return 0;
        }
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
        return contentType == AssetProcessingContentType.MEDITATION
                || contentType == AssetProcessingContentType.LULLABY;
    }

    private static String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static void validateKindTarget(AssetProcessingKind kind, AssetProcessingTarget target,
            AssetProcessingContentType contentType) {
        if (kind == AssetProcessingKind.STORY_NARRATION
                && (!target.isLocalization() || contentType != AssetProcessingContentType.STORY)) {
            throw new IllegalArgumentException("Story narration processing requires a STORY localization target");
        }
        if (kind == AssetProcessingKind.STORY_PAGE_MEDIA
                && (!target.isLocalization() || contentType != AssetProcessingContentType.STORY)) {
            throw new IllegalArgumentException("Story page media processing requires a STORY localization target");
        }
    }

    private static void validatePageSources(AssetProcessingKind kind, AssetProcessingTarget target,
            AssetProcessingContentType contentType, List<AssetProcessingPageSource> pageSources) {
        if (kind == AssetProcessingKind.STORY_PAGE_MEDIA) {
            if (!target.isLocalization() || contentType != AssetProcessingContentType.STORY || pageSources.isEmpty()) {
                throw new IllegalArgumentException("Story page media processing requires an ordered page source snapshot");
            }
            int previousPageNumber = 0;
            for (AssetProcessingPageSource source : pageSources) {
                if (source == null || source.pageNumber() <= previousPageNumber) {
                    throw new IllegalArgumentException("Story page source snapshot must be ordered by page number");
                }
                previousPageNumber = source.pageNumber();
            }
        } else if (!pageSources.isEmpty()) {
            throw new IllegalArgumentException("Page source snapshots are only supported for story page media processing");
        }
    }
}
