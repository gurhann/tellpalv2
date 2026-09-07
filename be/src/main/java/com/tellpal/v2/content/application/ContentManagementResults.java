package com.tellpal.v2.content.application;

import java.time.Instant;

import com.tellpal.v2.content.domain.LocalizationStatus;
import com.tellpal.v2.content.domain.ProcessingStatus;
import com.tellpal.v2.shared.domain.LanguageCode;

/**
 * Result types returned by content management application services.
 */
public final class ContentManagementResults {

    private ContentManagementResults() {
    }

    /**
     * Snapshot of one content localization after a management operation.
     */
    public record ContentLocalizationRecord(
            Long contentId,
            LanguageCode languageCode,
            String title,
            String description,
            String bodyText,
            Long coverMediaId,
            Long audioMediaId,
            Integer durationMinutes,
            LocalizationStatus status,
            ProcessingStatus processingStatus,
            Instant publishedAt,
            boolean visibleToMobile,
            StoryNarrationRecord narration) {

        public ContentLocalizationRecord(
                Long contentId,
                LanguageCode languageCode,
                String title,
                String description,
                String bodyText,
                Long coverMediaId,
                Long audioMediaId,
                Integer durationMinutes,
                LocalizationStatus status,
                ProcessingStatus processingStatus,
                Instant publishedAt,
                boolean visibleToMobile) {
            this(contentId, languageCode, title, description, bodyText, coverMediaId, audioMediaId,
                    durationMinutes, status, processingStatus, publishedAt, visibleToMobile, null);
        }

        public ContentLocalizationRecord {
            contentId = requirePositiveId(contentId, "Content ID must be positive");
            languageCode = requireLanguageCode(languageCode);
            title = requireText(title, "Content localization title must not be blank");
            status = requireLocalizationStatus(status);
            processingStatus = requireProcessingStatus(processingStatus);
        }
    }

    /** Snapshot of the shared lullaby playback and its content-scoped processing state. */
    public record LullabyPlaybackRecord(
            Long contentId,
            Long audioMediaId,
            Integer durationMinutes,
            ProcessingStatus processingStatus,
            String processingError) {
        public LullabyPlaybackRecord {
            contentId = requirePositiveId(contentId, "Content ID must be positive");
            audioMediaId = requirePositiveId(audioMediaId, "Lullaby audio media ID must be positive");
            if (durationMinutes == null || durationMinutes < 0) {
                throw new IllegalArgumentException("Lullaby duration minutes must be non-negative");
            }
        }
    }

    /** Locale-resolved active catalog option returned to admin editors. */
    public record InstrumentCatalogRecord(
            Long instrumentId,
            String code,
            String displayName) {
        public InstrumentCatalogRecord {
            instrumentId = requirePositiveId(instrumentId, "Instrument catalog ID must be positive");
            code = requireText(code, "Instrument catalog code must not be blank");
            displayName = requireText(displayName, "Instrument catalog display name must not be blank");
        }
    }

    /** Ordered instrument selection attached to a content-level lullaby playback. */
    public record LullabyInstrumentRecord(
            Long instrumentId,
            String code,
            String displayName,
            int displayOrder) {
        public LullabyInstrumentRecord {
            instrumentId = requirePositiveId(instrumentId, "Instrument catalog ID must be positive");
            code = requireText(code, "Instrument catalog code must not be blank");
            if (displayName != null) {
                displayName = displayName.trim();
            }
            if (displayOrder < 0) {
                throw new IllegalArgumentException("Lullaby instrument display order must not be negative");
            }
        }
    }

    /** Source snapshot of a story localization's optional full narration. */
    public record StoryNarrationRecord(
            Long audioMediaId,
            Integer durationMinutes,
            String processingStatus,
            String processingError) {

        public StoryNarrationRecord(Long audioMediaId, Integer durationMinutes) {
            this(audioMediaId, durationMinutes, null, null);
        }

        public StoryNarrationRecord {
            if (audioMediaId == null || audioMediaId <= 0) {
                throw new IllegalArgumentException("Narration audio media ID must be positive");
            }
            if (durationMinutes == null || durationMinutes < 0) {
                throw new IllegalArgumentException("Narration duration minutes must be non-negative");
            }
        }
    }

    /**
     * Snapshot of one story page after a management operation.
     */
    public record StoryPageRecord(
            Long contentId,
            int pageNumber,
            Long textlessIllustrationMediaId,
            int localizationCount) {

        public StoryPageRecord {
            contentId = requirePositiveId(contentId, "Content ID must be positive");
            pageNumber = requirePositiveNumber(pageNumber, "Story page number must be positive");
            textlessIllustrationMediaId = normalizePositiveId(
                    textlessIllustrationMediaId,
                    "Textless illustration media ID must be positive");
            if (localizationCount < 0) {
                throw new IllegalArgumentException("Localization count must not be negative");
            }
        }
    }

    /**
     * Snapshot of one localized story page after a management operation.
     */
    public record StoryPageLocalizationRecord(
            Long contentId,
            int pageNumber,
            LanguageCode languageCode,
            String bodyText,
            Long audioMediaId,
            Long illustrationMediaId) {

        public StoryPageLocalizationRecord {
            contentId = requirePositiveId(contentId, "Content ID must be positive");
            pageNumber = requirePositiveNumber(pageNumber, "Story page number must be positive");
            languageCode = requireLanguageCode(languageCode);
            audioMediaId = normalizePositiveId(audioMediaId, "Audio media ID must be positive");
            illustrationMediaId = normalizePositiveId(
                    illustrationMediaId,
                    "Illustration media ID must be positive");
        }
    }

    private static Long requirePositiveId(Long value, String message) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static int requirePositiveNumber(int value, String message) {
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static LanguageCode requireLanguageCode(LanguageCode languageCode) {
        if (languageCode == null) {
            throw new IllegalArgumentException("Language code must not be null");
        }
        return languageCode;
    }

    private static LocalizationStatus requireLocalizationStatus(LocalizationStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Localization status must not be null");
        }
        return status;
    }

    private static ProcessingStatus requireProcessingStatus(ProcessingStatus processingStatus) {
        if (processingStatus == null) {
            throw new IllegalArgumentException("Processing status must not be null");
        }
        return processingStatus;
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
}
