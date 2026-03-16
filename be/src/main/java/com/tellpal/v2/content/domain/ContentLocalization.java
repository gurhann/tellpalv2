package com.tellpal.v2.content.domain;

import com.tellpal.v2.shared.domain.LocalizationStatus;
import com.tellpal.v2.shared.domain.ProcessingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "v2_content_localizations")
public class ContentLocalization {

    @EmbeddedId
    private ContentLocalizationId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("contentId")
    @JoinColumn(name = "content_id", nullable = false)
    private Content content;

    @Column(name = "title", nullable = false)
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
    @Column(name = "status", nullable = false, length = 16)
    private LocalizationStatus status = LocalizationStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 16)
    private ProcessingStatus processingStatus = ProcessingStatus.PENDING;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected ContentLocalization() {
    }

    public ContentLocalization(Content content, String languageCode, String title) {
        this.content = content;
        this.id = new ContentLocalizationId(content.getId(), languageCode);
        this.title = title;
        this.status = LocalizationStatus.DRAFT;
        this.processingStatus = ProcessingStatus.PENDING;
    }

    public ContentLocalizationId getId() {
        return id;
    }

    public Content getContent() {
        return content;
    }

    public String getLanguageCode() {
        return id.getLanguageCode();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBodyText() {
        return bodyText;
    }

    public void setBodyText(String bodyText) {
        this.bodyText = bodyText;
    }

    public Long getCoverMediaId() {
        return coverMediaId;
    }

    public void setCoverMediaId(Long coverMediaId) {
        this.coverMediaId = coverMediaId;
    }

    public Long getAudioMediaId() {
        return audioMediaId;
    }

    public void setAudioMediaId(Long audioMediaId) {
        this.audioMediaId = audioMediaId;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public LocalizationStatus getStatus() {
        return status;
    }

    public void setStatus(LocalizationStatus status) {
        this.status = status;
    }

    public ProcessingStatus getProcessingStatus() {
        return processingStatus;
    }

    public void setProcessingStatus(ProcessingStatus processingStatus) {
        this.processingStatus = processingStatus;
    }

    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(OffsetDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
