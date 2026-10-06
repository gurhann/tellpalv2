package com.tellpal.v2.asset.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import com.tellpal.v2.shared.domain.LanguageCode;
import com.tellpal.v2.shared.infrastructure.persistence.BaseJpaEntity;
import com.tellpal.v2.asset.api.AssetProcessingTarget;
import com.tellpal.v2.asset.api.AssetProcessingTargetScope;
import com.tellpal.v2.asset.api.AssetProcessingKind;
import com.tellpal.v2.asset.api.AssetProcessingPageSource;

/**
 * Aggregate root that tracks the asset processing lifecycle for one typed target.
 *
 * <p>The aggregate owns source asset context, retry timing, worker lease state, and the transition
 * history needed to move between pending, processing, failed, and completed states.
 */
@Entity
@Table(name = "asset_processing")
public class AssetProcessing extends BaseJpaEntity {

    @Column(name = "content_id", nullable = false)
    private Long contentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_kind", nullable = false, length = 30)
    private AssetProcessingKind kind;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_scope", nullable = false, length = 20)
    private AssetProcessingTargetScope targetScope;

    @Column(name = "language_code", length = 8)
    private LanguageCode languageCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", length = 20)
    private ProcessingContentType contentType;

    @Column(name = "external_key", length = 180)
    private String externalKey;

    @Column(name = "cover_source_asset_id")
    private Long coverSourceAssetId;

    @Column(name = "listing_profile_version", length = 180)
    private String listingProfileVersion;

    public String getListingProfileVersion() { return listingProfileVersion; }

    /** Captures the immutable transformation profile before the worker acquires its lease. */
    public void setListingProfileVersion(String profile) {
        if (kind != AssetProcessingKind.LISTING_COVER || status == AssetProcessingStatus.PROCESSING)
            throw new IllegalStateException("Listing profile can only change outside an active lease");
        listingProfileVersion = requireText(profile,"Listing profile is required");
    }

    /** Rejects late callbacks before any job or ready-manifest mutation. */
    public void requireListingOwnership(com.tellpal.v2.asset.api.ListingCoverAttempt attempt, Instant now) {
        if (kind != AssetProcessingKind.LISTING_COVER || attempt == null
                || status != AssetProcessingStatus.PROCESSING
                || !java.util.Objects.equals(getId(),attempt.processingId())
                || attemptCount != attempt.attemptCount()
                || leaseExpiresAt == null || !leaseExpiresAt.equals(attempt.leaseExpiresAt()) || !leaseExpiresAt.isAfter(now)
                || !java.util.Objects.equals(coverSourceAssetId,attempt.sourceAssetId())
                || !java.util.Objects.equals(listingProfileVersion,attempt.profileVersion()))
            throw new com.tellpal.v2.asset.api.StaleListingCoverAttemptException();
    }

    @Column(name = "audio_source_asset_id")
    private Long audioSourceAssetId;

    @Column(name = "page_count")
    private Integer pageCount;

    @Column(name = "page_source_snapshot", columnDefinition = "text")
    private String pageSourceSnapshot;

    @Column(name = "page_source_followup_snapshot", columnDefinition = "text")
    private String pageSourceFollowupSnapshot;

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
            AssetProcessingTarget target,
            AssetProcessingKind kind,
            ProcessingContentType contentType,
            String externalKey,
            Long coverSourceAssetId,
            Long audioSourceAssetId,
            Integer pageCount,
            List<AssetProcessingPageSource> pageSources,
            Instant nextAttemptAt) {
        this.targetScope = requireTarget(target).scope();
        this.kind = requireKind(kind);
        validateKindTarget(this.kind, target, contentType);
        this.contentId = target.contentId();
        this.languageCode = target.languageCode();
        refreshContext(contentType, externalKey, coverSourceAssetId, audioSourceAssetId, pageCount, pageSources);
        this.status = AssetProcessingStatus.PENDING;
        this.attemptCount = 0;
        this.nextAttemptAt = requireInstant(nextAttemptAt, "Next attempt time must not be null");
    }

    public static AssetProcessing schedule(
            AssetProcessingTarget target,
            AssetProcessingKind kind,
            ProcessingContentType contentType,
            String externalKey,
            Long coverSourceAssetId,
            Long audioSourceAssetId,
            Integer pageCount,
            Instant nextAttemptAt) {
        return new AssetProcessing(
                target,
                kind,
                contentType,
                externalKey,
                coverSourceAssetId,
                audioSourceAssetId,
                pageCount,
                List.of(),
                nextAttemptAt);
    }

    public static AssetProcessing schedule(
            AssetProcessingTarget target,
            AssetProcessingKind kind,
            ProcessingContentType contentType,
            String externalKey,
            Long coverSourceAssetId,
            Long audioSourceAssetId,
            Integer pageCount,
            List<AssetProcessingPageSource> pageSources,
            Instant nextAttemptAt) {
        return new AssetProcessing(target, kind, contentType, externalKey, coverSourceAssetId,
                audioSourceAssetId, pageCount, pageSources, nextAttemptAt);
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
                AssetProcessingTarget.localization(contentId, languageCode),
                AssetProcessingKind.DELIVERY,
                contentType,
                externalKey,
                coverSourceAssetId,
                audioSourceAssetId,
                pageCount,
                List.of(),
                nextAttemptAt);
    }

    public Long getContentId() {
        return contentId;
    }

    public AssetProcessingKind getKind() {
        return kind;
    }

    public LanguageCode getLanguageCode() {
        return languageCode;
    }

    public AssetProcessingTargetScope getTargetScope() {
        return targetScope;
    }

    public AssetProcessingTarget getTarget() {
        return new AssetProcessingTarget(targetScope, contentId, languageCode);
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

    public List<AssetProcessingPageSource> getPageSources() {
        return decodePageSources(pageSourceSnapshot);
    }

    public List<AssetProcessingPageSource> getPageSourceFollowup() {
        return decodePageSources(pageSourceFollowupSnapshot);
    }

    /** Coalesces a newer localized page snapshot while the current worker lease remains active. */
    public void queuePageSourceFollowup(List<AssetProcessingPageSource> pageSources) {
        requireStatus(AssetProcessingStatus.PROCESSING);
        if (kind != AssetProcessingKind.STORY_PAGE_MEDIA) {
            throw new IllegalStateException("Only story page media processing accepts a follow-up snapshot");
        }
        if (pageSources == null || pageSources.isEmpty()) {
            throw new IllegalArgumentException("Story page media follow-up snapshot must not be empty");
        }
        this.pageSourceFollowupSnapshot = encodePageSources(pageSources);
    }

    public void clearPageSourceFollowup() {
        requireStatus(AssetProcessingStatus.PROCESSING);
        if (kind != AssetProcessingKind.STORY_PAGE_MEDIA) {
            throw new IllegalStateException("Only story page media processing accepts a follow-up snapshot");
        }
        this.pageSourceFollowupSnapshot = null;
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

    /**
     * Replaces the source context used by the next processing attempt.
     *
     * <p>Non-story content requires a single audio source asset, while story processing owns page
     * count instead.
     */
    public void refreshContext(
            ProcessingContentType contentType,
            String externalKey,
            Long coverSourceAssetId,
            Long audioSourceAssetId,
            Integer pageCount) {
        refreshContext(contentType, externalKey, coverSourceAssetId, audioSourceAssetId, pageCount, List.of());
    }

    public void refreshContext(
            ProcessingContentType contentType,
            String externalKey,
            Long coverSourceAssetId,
            Long audioSourceAssetId,
            Integer pageCount,
            List<AssetProcessingPageSource> pageSources) {
        ProcessingContentType requiredContentType = requireContentType(contentType);
        String requiredExternalKey = requireText(externalKey, "External key must not be blank");
        Long normalizedCoverSourceAssetId = normalizePositiveId(coverSourceAssetId, "Cover source asset ID must be positive");
        Long normalizedAudioSourceAssetId = normalizePositiveId(audioSourceAssetId, "Audio source asset ID must be positive");
        Integer normalizedPageCount = normalizePageCount(kind, requiredContentType, pageCount);

        if (kind == AssetProcessingKind.STORY_NARRATION && normalizedAudioSourceAssetId == null) {
            throw new IllegalArgumentException("Narration audio source asset ID is required");
        }
        if (requiredContentType.requiresSingleAudioAsset() && normalizedAudioSourceAssetId == null) {
            throw new IllegalArgumentException("Audio source asset ID is required for non-story processing");
        }

        this.contentType = requiredContentType;
        this.externalKey = requiredExternalKey;
        this.coverSourceAssetId = normalizedCoverSourceAssetId;
        this.audioSourceAssetId = normalizedAudioSourceAssetId;
        this.pageCount = normalizedPageCount;
        this.pageSourceSnapshot = encodePageSources(pageSources);
        this.pageSourceFollowupSnapshot = null;
    }

    private static String encodePageSources(List<AssetProcessingPageSource> pageSources) {
        if (pageSources == null || pageSources.isEmpty()) return null;
        return pageSources.stream().map(source -> String.join(":",
                source.pageId().toString(), Integer.toString(source.pageNumber()),
                source.illustrationAssetId() == null ? "" : source.illustrationAssetId().toString(),
                source.audioAssetId() == null ? "" : source.audioAssetId().toString()))
                .collect(java.util.stream.Collectors.joining(";"));
    }

    private static List<AssetProcessingPageSource> decodePageSources(String snapshot) {
        if (snapshot == null || snapshot.isBlank()) return List.of();
        return java.util.Arrays.stream(snapshot.split(";"))
                .map(row -> row.split(":", -1))
                .map(fields -> new AssetProcessingPageSource(
                        Long.valueOf(fields[0]), Integer.parseInt(fields[1]),
                        fields[2].isBlank() ? null : Long.valueOf(fields[2]),
                        fields[3].isBlank() ? null : Long.valueOf(fields[3])))
                .toList();
    }

    /**
     * Starts processing and acquires a lease for the worker.
     */
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

    /**
     * Marks the current processing attempt as completed.
     */
    public void complete(Instant completedAt) {
        requireStatus(AssetProcessingStatus.PROCESSING);
        Instant requiredCompletedAt = requireInstant(completedAt, "Completion time must not be null");
        this.status = AssetProcessingStatus.COMPLETED;
        this.completedAt = requiredCompletedAt;
        this.failedAt = null;
        this.leaseExpiresAt = null;
        clearFailureDetails();
    }

    /** Completes the active snapshot or immediately queues the latest follow-up snapshot. */
    public void completeOrQueueFollowup(Instant completedAt) {
        if (pageSourceFollowupSnapshot == null || pageSourceFollowupSnapshot.isBlank()) {
            complete(completedAt);
            return;
        }
        requireStatus(AssetProcessingStatus.PROCESSING);
        Instant at = requireInstant(completedAt, "Completion time must not be null");
        this.pageSourceSnapshot = pageSourceFollowupSnapshot;
        this.pageSourceFollowupSnapshot = null;
        this.status = AssetProcessingStatus.PENDING;
        this.nextAttemptAt = at;
        this.completedAt = null;
        this.failedAt = null;
        this.leaseExpiresAt = null;
        this.startedAt = null;
        clearFailureDetails();
    }

    /**
     * Marks the current processing attempt as failed with diagnostic details.
     */
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

    /** Drops a failed active attempt in favor of a newer snapshot when one arrived during processing. */
    public void failOrQueueFollowup(String errorCode, String errorMessage, Instant failedAt) {
        if (pageSourceFollowupSnapshot == null || pageSourceFollowupSnapshot.isBlank()) {
            fail(errorCode, errorMessage, failedAt);
            return;
        }
        requireStatus(AssetProcessingStatus.PROCESSING);
        Instant at = requireInstant(failedAt, "Failure time must not be null");
        this.pageSourceSnapshot = pageSourceFollowupSnapshot;
        this.pageSourceFollowupSnapshot = null;
        this.status = AssetProcessingStatus.PENDING;
        this.nextAttemptAt = at;
        this.completedAt = null;
        this.failedAt = null;
        this.leaseExpiresAt = null;
        this.startedAt = null;
        clearFailureDetails();
    }

    /**
     * Moves a failed entry back to pending so a later worker can try again.
     */
    public void retry(Instant nextAttemptAt) {
        requireStatus(AssetProcessingStatus.FAILED);
        this.status = AssetProcessingStatus.PENDING;
        this.nextAttemptAt = requireInstant(nextAttemptAt, "Next attempt time must not be null");
        this.leaseExpiresAt = null;
        this.completedAt = null;
        this.failedAt = null;
    }

    /** Re-queues terminal work after a source asset was replaced. */
    public void reschedule(Instant nextAttemptAt) {
        if (!isTerminal()) {
            throw new IllegalStateException("Only terminal asset processing can be rescheduled");
        }
        this.status = AssetProcessingStatus.PENDING;
        this.nextAttemptAt = requireInstant(nextAttemptAt, "Next attempt time must not be null");
        this.leaseExpiresAt = null;
        this.completedAt = null;
        this.failedAt = null;
        clearFailureDetails();
    }

    /**
     * Releases an expired worker lease and returns the entry to pending.
     */
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

    private static AssetProcessingTarget requireTarget(AssetProcessingTarget target) {
        if (target == null) {
            throw new IllegalArgumentException("Asset processing target must not be null");
        }
        return target;
    }

    private static AssetProcessingKind requireKind(AssetProcessingKind kind) {
        if (kind == null) {
            throw new IllegalArgumentException("Processing kind must not be null");
        }
        return kind;
    }

    private static void validateKindTarget(AssetProcessingKind kind, AssetProcessingTarget target,
            ProcessingContentType contentType) {
        if ((kind == AssetProcessingKind.STORY_NARRATION || kind == AssetProcessingKind.LISTING_COVER)
                && (!target.isLocalization() || contentType != ProcessingContentType.STORY)) {
            throw new IllegalArgumentException("Story narration processing requires a STORY localization target");
        }
        if (kind == AssetProcessingKind.STORY_PAGE_MEDIA
                && (!target.isLocalization() || contentType != ProcessingContentType.STORY)) {
            throw new IllegalArgumentException("Story page media processing requires a STORY localization target");
        }
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

    private static Integer normalizePageCount(AssetProcessingKind kind,
            ProcessingContentType contentType, Integer pageCount) {
        if (kind == AssetProcessingKind.STORY_NARRATION || kind == AssetProcessingKind.LISTING_COVER) {
            if (pageCount != null && pageCount != 0) {
                throw new IllegalArgumentException("Page count is not supported for story narration processing");
            }
            return 0;
        }
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
