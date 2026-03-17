package com.tellpal.v2.content.application;

import java.time.Instant;

import com.tellpal.v2.content.domain.ContentType;
import com.tellpal.v2.content.domain.LocalizationStatus;
import com.tellpal.v2.content.domain.ProcessingStatus;
import com.tellpal.v2.shared.domain.LanguageCode;

public final class ContentManagementCommands {

    private ContentManagementCommands() {
    }

    public record CreateContentCommand(ContentType type, String externalKey, Integer ageRange, boolean active) {

        public CreateContentCommand {
            type = requireContentType(type);
            externalKey = requireText(externalKey, "Content external key must not be blank");
            ageRange = normalizeNonNegative(ageRange, "Content age range must not be negative");
        }
    }

    public record UpdateContentCommand(Long contentId, String externalKey, Integer ageRange, boolean active) {

        public UpdateContentCommand {
            contentId = requirePositiveId(contentId, "Content ID must be positive");
            externalKey = requireText(externalKey, "Content external key must not be blank");
            ageRange = normalizeNonNegative(ageRange, "Content age range must not be negative");
        }
    }

    public record CreateContentLocalizationCommand(
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
            Instant publishedAt) {

        public CreateContentLocalizationCommand {
            validateLocalizationCommand(
                    contentId,
                    languageCode,
                    title,
                    coverMediaId,
                    audioMediaId,
                    durationMinutes,
                    status,
                    processingStatus,
                    publishedAt);
        }
    }

    public record UpdateContentLocalizationCommand(
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
            Instant publishedAt) {

        public UpdateContentLocalizationCommand {
            validateLocalizationCommand(
                    contentId,
                    languageCode,
                    title,
                    coverMediaId,
                    audioMediaId,
                    durationMinutes,
                    status,
                    processingStatus,
                    publishedAt);
        }
    }

    public record MarkContentLocalizationProcessingCommand(
            Long contentId,
            LanguageCode languageCode,
            ProcessingStatus processingStatus) {

        public MarkContentLocalizationProcessingCommand {
            contentId = requirePositiveId(contentId, "Content ID must be positive");
            languageCode = requireLanguageCode(languageCode);
            processingStatus = requireProcessingStatus(processingStatus);
        }
    }

    public record AddStoryPageCommand(Long contentId, int pageNumber, Long illustrationMediaId) {

        public AddStoryPageCommand {
            contentId = requirePositiveId(contentId, "Content ID must be positive");
            pageNumber = requirePositiveNumber(pageNumber, "Story page number must be positive");
            illustrationMediaId = normalizePositiveId(illustrationMediaId, "Illustration media ID must be positive");
        }
    }

    public record UpdateStoryPageCommand(Long contentId, int pageNumber, Long illustrationMediaId) {

        public UpdateStoryPageCommand {
            contentId = requirePositiveId(contentId, "Content ID must be positive");
            pageNumber = requirePositiveNumber(pageNumber, "Story page number must be positive");
            illustrationMediaId = normalizePositiveId(illustrationMediaId, "Illustration media ID must be positive");
        }
    }

    public record RemoveStoryPageCommand(Long contentId, int pageNumber) {

        public RemoveStoryPageCommand {
            contentId = requirePositiveId(contentId, "Content ID must be positive");
            pageNumber = requirePositiveNumber(pageNumber, "Story page number must be positive");
        }
    }

    public record UpsertStoryPageLocalizationCommand(
            Long contentId,
            int pageNumber,
            LanguageCode languageCode,
            String bodyText,
            Long audioMediaId) {

        public UpsertStoryPageLocalizationCommand {
            contentId = requirePositiveId(contentId, "Content ID must be positive");
            pageNumber = requirePositiveNumber(pageNumber, "Story page number must be positive");
            languageCode = requireLanguageCode(languageCode);
            audioMediaId = normalizePositiveId(audioMediaId, "Audio media ID must be positive");
        }
    }

    private static void validateLocalizationCommand(
            Long contentId,
            LanguageCode languageCode,
            String title,
            Long coverMediaId,
            Long audioMediaId,
            Integer durationMinutes,
            LocalizationStatus status,
            ProcessingStatus processingStatus,
            Instant publishedAt) {
        requirePositiveId(contentId, "Content ID must be positive");
        requireLanguageCode(languageCode);
        requireText(title, "Content localization title must not be blank");
        normalizePositiveId(coverMediaId, "Cover media ID must be positive");
        normalizePositiveId(audioMediaId, "Audio media ID must be positive");
        normalizeNonNegative(durationMinutes, "Duration minutes must not be negative");
        requireLocalizationStatus(status, publishedAt);
        requireProcessingStatus(processingStatus);
    }

    private static ContentType requireContentType(ContentType type) {
        if (type == null) {
            throw new IllegalArgumentException("Content type must not be null");
        }
        return type;
    }

    private static ProcessingStatus requireProcessingStatus(ProcessingStatus processingStatus) {
        if (processingStatus == null) {
            throw new IllegalArgumentException("Processing status must not be null");
        }
        return processingStatus;
    }

    private static void requireLocalizationStatus(LocalizationStatus status, Instant publishedAt) {
        if (status == null) {
            throw new IllegalArgumentException("Localization status must not be null");
        }
        if (status == LocalizationStatus.PUBLISHED && publishedAt == null) {
            throw new IllegalArgumentException("Published localization must include a publish timestamp");
        }
    }

    private static LanguageCode requireLanguageCode(LanguageCode languageCode) {
        if (languageCode == null) {
            throw new IllegalArgumentException("Language code must not be null");
        }
        return languageCode;
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

    private static Long normalizePositiveId(Long value, String message) {
        if (value == null) {
            return null;
        }
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static Integer normalizeNonNegative(Integer value, String message) {
        if (value == null) {
            return null;
        }
        if (value < 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
