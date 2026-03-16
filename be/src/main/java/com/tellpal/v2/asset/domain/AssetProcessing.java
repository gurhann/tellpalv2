package com.tellpal.v2.asset.domain;

import com.tellpal.v2.shared.infrastructure.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "v2_asset_processing")
public class AssetProcessing extends BaseEntity {

    @Column(name = "content_id", nullable = false)
    private Long contentId;

    @Column(name = "language_code", nullable = false, length = 10)
    private String languageCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ProcessingStatus status = ProcessingStatus.PENDING;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    protected AssetProcessing() {
    }

    public AssetProcessing(Long contentId, String languageCode) {
        this.contentId = contentId;
        this.languageCode = languageCode;
        this.status = ProcessingStatus.PENDING;
    }

    public void startProcessing() {
        this.status = this.status.transitionTo(ProcessingStatus.PROCESSING);
        this.startedAt = OffsetDateTime.now();
    }

    public void completeProcessing() {
        this.status = this.status.transitionTo(ProcessingStatus.COMPLETED);
        this.completedAt = OffsetDateTime.now();
    }

    public void failProcessing(String errorMessage) {
        this.status = this.status.transitionTo(ProcessingStatus.FAILED);
        this.errorMessage = errorMessage;
    }

    public void resetForRetry() {
        this.status = this.status.transitionTo(ProcessingStatus.PENDING);
        this.startedAt = null;
        this.completedAt = null;
        this.errorMessage = null;
    }

    public Long getContentId() {
        return contentId;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public ProcessingStatus getStatus() {
        return status;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
