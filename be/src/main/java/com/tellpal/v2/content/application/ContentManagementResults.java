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
            boolean visibleToMobile) {

        public ContentLocalizationRecord {
            contentId = requirePositiveId(contentId, "Content ID must be positive");
            languageCode = requireLanguageCode(languageCode);
            title = requireText(title, "Content localization title must not be blank");
            status = requireLocalizationStatus(status);
            processingStatus = requireProcessingStatus(processingStatus);
        }
    }

    /**
     * Snapshot of one story page after a management operation.
     */
    public record StoryPageRecord(Long contentId, int pageNumber, Long illustrationMediaId, int localizationCount) {

        public StoryPageRecord {
            contentId = requirePositiveId(contentId, "Content ID must be positive");
            pageNumber = requirePositiveNumber(pageNumber, "Story page number must be positive");
            illustrationMediaId = normalizePositiveId(
                    illustrationMediaId,
                    "Illustration media ID must be positive");
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
            Long audioMediaId) {

        public StoryPageLocalizationRecord {
            contentId = requirePositiveId(contentId, "Content ID must be positive");
            pageNumber = requirePositiveNumber(pageNumber, "Story page number must be positive");
            languageCode = requireLanguageCode(languageCode);
            audioMediaId = normalizePositiveId(audioMediaId, "Audio media ID must be positive");
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
