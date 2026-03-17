package com.tellpal.v2.asset.domain;

import java.time.Duration;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import com.tellpal.v2.shared.domain.LanguageCode;
import com.tellpal.v2.shared.infrastructure.persistence.BaseJpaEntity;

@Entity
@Table(name = "asset_processing")
public class AssetProcessing extends BaseJpaEntity {

    @Column(name = "content_id", nullable = false)
    private Long contentId;

    @Column(name = "language_code", nullable = false, length = 8)
    private LanguageCode languageCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", length = 20)
    private ProcessingContentType contentType;

    @Column(name = "external_key", length = 180)
    private String externalKey;

    @Column(name = "cover_source_asset_id")
    private Long coverSourceAssetId;

    @Column(name = "audio_source_asset_id")
    private Long audioSourceAssetId;

    @Column(name = "page_count")
    private Integer pageCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AssetProcessingStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "lease_expires_at")
    private Instant leaseExpiresAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "last_error_code", length = 100)
    private String lastErrorCode;

    @Column(name = "last_error_message")
    private String lastErrorMessage;

    protected AssetProcessing() {
    }

    private AssetProcessing(
            Long contentId,
            LanguageCode languageCode,
            ProcessingContentType contentType,
            String externalKey,
            Long coverSourceAssetId,
            Long audioSourceAssetId,
            Integer pageCount,
            Instant nextAttemptAt) {
        this.contentId = requirePositiveId(contentId, "Content ID must be positive");
        this.languageCode = requireLanguageCode(languageCode);
        refreshContext(contentType, externalKey, coverSourceAssetId, audioSourceAssetId, pageCount);
        this.status = AssetProcessingStatus.PENDING;
        this.attemptCount = 0;
        this.nextAttemptAt = requireInstant(nextAttemptAt, "Next attempt time must not be null");
    }

    public static AssetProcessing schedule(
            Long contentId,
            LanguageCode languageCode,
            ProcessingContentType contentType,
            String externalKey,
            Long coverSourceAssetId,
            Long audioSourceAssetId,
            Integer pageCount,
            Instant nextAttemptAt) {
        return new AssetProcessing(
                contentId,
                languageCode,
                contentType,
                externalKey,
                coverSourceAssetId,
                audioSourceAssetId,
                pageCount,
                nextAttemptAt);
    }

    public Long getContentId() {
        return contentId;
    }

    public LanguageCode getLanguageCode() {
        return languageCode;
    }

    public ProcessingContentType getContentType() {
        return contentType;
    }

    public String getExternalKey() {
        return externalKey;
    }

    public Long getCoverSourceAssetId() {
        return coverSourceAssetId;
    }

    public Long getAudioSourceAssetId() {
        return audioSourceAssetId;
    }

    public Integer getPageCount() {
        return pageCount;
    }

    public AssetProcessingStatus getStatus() {
        return status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public Instant getLeaseExpiresAt() {
        return leaseExpiresAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getFailedAt() {
        return failedAt;
    }

    public String getLastErrorCode() {
        return lastErrorCode;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public boolean isTerminal() {
        return status == AssetProcessingStatus.COMPLETED || status == AssetProcessingStatus.FAILED;
    }

    public boolean isRetryable() {
        return status == AssetProcessingStatus.FAILED;
    }

    public boolean isDueAt(Instant referenceTime) {
        Instant requiredReferenceTime = requireInstant(referenceTime, "Reference time must not be null");
        return status == AssetProcessingStatus.PENDING && !nextAttemptAt.isAfter(requiredReferenceTime);
    }

    public boolean hasExpiredLeaseAt(Instant referenceTime) {
        Instant requiredReferenceTime = requireInstant(referenceTime, "Reference time must not be null");
        return status == AssetProcessingStatus.PROCESSING
                && leaseExpiresAt != null
                && !leaseExpiresAt.isAfter(requiredReferenceTime);
    }

    public void refreshContext(
            ProcessingContentType contentType,
            String externalKey,
            Long coverSourceAssetId,
            Long audioSourceAssetId,
            Integer pageCount) {
        ProcessingContentType requiredContentType = requireContentType(contentType);
        String requiredExternalKey = requireText(externalKey, "External key must not be blank");
        Long normalizedCoverSourceAssetId = normalizePositiveId(coverSourceAssetId, "Cover source asset ID must be positive");
        Long normalizedAudioSourceAssetId = normalizePositiveId(audioSourceAssetId, "Audio source asset ID must be positive");
        Integer normalizedPageCount = normalizePageCount(requiredContentType, pageCount);

        if (requiredContentType.requiresSingleAudioAsset() && normalizedAudioSourceAssetId == null) {
            throw new IllegalArgumentException("Audio source asset ID is required for non-story processing");
        }

        this.contentType = requiredContentType;
        this.externalKey = requiredExternalKey;
        this.coverSourceAssetId = normalizedCoverSourceAssetId;
        this.audioSourceAssetId = normalizedAudioSourceAssetId;
        this.pageCount = normalizedPageCount;
    }

    public void start(Instant startedAt, Duration leaseDuration) {
        requireStatus(AssetProcessingStatus.PENDING);
        Instant requiredStartedAt = requireInstant(startedAt, "Start time must not be null");
        Duration requiredLeaseDuration = requirePositiveDuration(leaseDuration);
        this.status = AssetProcessingStatus.PROCESSING;
        this.attemptCount += 1;
        this.startedAt = requiredStartedAt;
        this.leaseExpiresAt = requiredStartedAt.plus(requiredLeaseDuration);
        this.completedAt = null;
        this.failedAt = null;
        this.nextAttemptAt = requiredStartedAt;
        clearFailureDetails();
    }

    public void complete(Instant completedAt) {
        requireStatus(AssetProcessingStatus.PROCESSING);
        Instant requiredCompletedAt = requireInstant(completedAt, "Completion time must not be null");
        this.status = AssetProcessingStatus.COMPLETED;
        this.completedAt = requiredCompletedAt;
        this.failedAt = null;
        this.leaseExpiresAt = null;
        clearFailureDetails();
    }

    public void fail(String errorCode, String errorMessage, Instant failedAt) {
        requireStatus(AssetProcessingStatus.PROCESSING);
        String normalizedErrorCode = normalizeOptionalText(errorCode);
        String normalizedErrorMessage = normalizeOptionalText(errorMessage);
        if (normalizedErrorCode == null && normalizedErrorMessage == null) {
            throw new IllegalArgumentException("Failure details must include an error code or message");
        }
        this.status = AssetProcessingStatus.FAILED;
        this.failedAt = requireInstant(failedAt, "Failure time must not be null");
        this.completedAt = null;
        this.leaseExpiresAt = null;
        this.lastErrorCode = normalizedErrorCode;
        this.lastErrorMessage = normalizedErrorMessage;
    }

    public void retry(Instant nextAttemptAt) {
        requireStatus(AssetProcessingStatus.FAILED);
        this.status = AssetProcessingStatus.PENDING;
        this.nextAttemptAt = requireInstant(nextAttemptAt, "Next attempt time must not be null");
        this.leaseExpiresAt = null;
        this.completedAt = null;
        this.failedAt = null;
    }

    public void recoverExpiredLease(Instant nextAttemptAt, String errorCode, String errorMessage) {
        requireStatus(AssetProcessingStatus.PROCESSING);
        this.status = AssetProcessingStatus.PENDING;
        this.nextAttemptAt = requireInstant(nextAttemptAt, "Next attempt time must not be null");
        this.leaseExpiresAt = null;
        this.completedAt = null;
        this.failedAt = null;
        this.lastErrorCode = normalizeOptionalText(errorCode);
        this.lastErrorMessage = normalizeOptionalText(errorMessage);
    }

    private void clearFailureDetails() {
        lastErrorCode = null;
        lastErrorMessage = null;
    }

    private void requireStatus(AssetProcessingStatus expectedStatus) {
        if (status != expectedStatus) {
            throw new IllegalStateException(
                    "Asset processing must be %s but was %s".formatted(expectedStatus, status));
        }
    }

    private static Long requirePositiveId(Long value, String message) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static ProcessingContentType requireContentType(ProcessingContentType contentType) {
        if (contentType == null) {
            throw new IllegalArgumentException("Processing content type must not be null");
        }
        return contentType;
    }

    private static LanguageCode requireLanguageCode(LanguageCode languageCode) {
        if (languageCode == null) {
            throw new IllegalArgumentException("Language code must not be null");
        }
        return languageCode;
    }

    private static Instant requireInstant(Instant value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static Duration requirePositiveDuration(Duration duration) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("Lease duration must be positive");
        }
        return duration;
    }

    private static String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
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

    private static Integer normalizePageCount(ProcessingContentType contentType, Integer pageCount) {
        if (contentType.supportsStoryPackages()) {
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
}
