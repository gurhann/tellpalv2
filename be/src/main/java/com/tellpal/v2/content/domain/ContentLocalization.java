package com.tellpal.v2.content.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.tellpal.v2.shared.domain.LanguageCode;
import com.tellpal.v2.shared.infrastructure.persistence.BaseJpaEntity;

@Entity
@Table(name = "content_localizations")
public class ContentLocalization extends BaseJpaEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "content_id", nullable = false)
    private Content content;

    @Column(name = "language_code", nullable = false, length = 8)
    private LanguageCode languageCode;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "body_text")
    private String bodyText;

    @Column(name = "cover_media_id")
    private Long coverMediaId;

    @Column(name = "audio_media_id")
    private Long audioMediaId;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LocalizationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 20)
    private ProcessingStatus processingStatus;

    @Column(name = "published_at")
    private Instant publishedAt;

    protected ContentLocalization() {
    }

    ContentLocalization(
            Content content,
            LanguageCode languageCode,
            String title,
            LocalizationStatus status,
            ProcessingStatus processingStatus) {
        this.content = requireContent(content);
        this.languageCode = requireLanguageCode(languageCode);
        this.title = requireText(title, "Content localization title must not be blank");
        this.status = requireStatus(status);
        this.processingStatus = requireProcessingStatus(processingStatus);
    }

    public LanguageCode getLanguageCode() {
        return languageCode;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getBodyText() {
        return bodyText;
    }

    public Long getCoverMediaId() {
        return coverMediaId;
    }

    public Long getAudioMediaId() {
        return audioMediaId;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public LocalizationStatus getStatus() {
        return status;
    }

    public ProcessingStatus getProcessingStatus() {
        return processingStatus;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void updateContent(
            String title,
            String description,
            String bodyText,
            Long coverMediaId,
            Long audioMediaId,
            Integer durationMinutes) {
        this.title = requireText(title, "Content localization title must not be blank");
        this.description = normalizeOptionalText(description);
        this.bodyText = normalizeOptionalText(bodyText);
        this.coverMediaId = normalizePositiveId(coverMediaId, "Cover media ID must be positive");
        this.audioMediaId = normalizePositiveId(audioMediaId, "Audio media ID must be positive");
        this.durationMinutes = normalizeNonNegative(durationMinutes, "Duration minutes must not be negative");
    }

    public void markStatus(LocalizationStatus status, Instant publishedAt) {
        LocalizationStatus nextStatus = requireStatus(status);
        if (nextStatus == LocalizationStatus.PUBLISHED && publishedAt == null) {
            throw new IllegalArgumentException("Published localization must include a publish timestamp");
        }
        this.status = nextStatus;
        this.publishedAt = nextStatus == LocalizationStatus.PUBLISHED ? publishedAt : this.publishedAt;
    }

    public void markProcessingStatus(ProcessingStatus processingStatus) {
        this.processingStatus = requireProcessingStatus(processingStatus);
    }

    private static Content requireContent(Content content) {
        if (content == null) {
            throw new IllegalArgumentException("Content must not be null");
        }
        return content;
    }

    private static LanguageCode requireLanguageCode(LanguageCode languageCode) {
        if (languageCode == null) {
            throw new IllegalArgumentException("Language code must not be null");
        }
        return languageCode;
    }

    private static LocalizationStatus requireStatus(LocalizationStatus status) {
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

    private static String requireText(String value, String message) {
        String normalized = normalizeOptionalText(value);
        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private static String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
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
