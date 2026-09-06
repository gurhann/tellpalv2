package com.tellpal.v2.asset.api;

import java.time.Instant;

import com.tellpal.v2.shared.domain.LanguageCode;

/**
 * Snapshot of the persisted processing state for one content localization.
 *
 * <p>The record exposes both workflow state and the source asset context needed to derive delivery
 * outputs.
 */
public record AssetProcessingRecord(
        Long processingId,
        AssetProcessingTarget target,
        AssetProcessingContentType contentType,
        String externalKey,
        Long coverSourceAssetId,
        Long audioSourceAssetId,
        Integer pageCount,
        AssetProcessingState status,
        int attemptCount,
        Instant nextAttemptAt,
        Instant leaseExpiresAt,
        Instant startedAt,
        Instant completedAt,
        Instant failedAt,
        String lastErrorCode,
        String lastErrorMessage,
        Instant createdAt,
        Instant updatedAt) {

    public AssetProcessingRecord(
            Long processingId,
            Long contentId,
            LanguageCode languageCode,
            AssetProcessingContentType contentType,
            String externalKey,
            Long coverSourceAssetId,
            Long audioSourceAssetId,
            Integer pageCount,
            AssetProcessingState status,
            int attemptCount,
            Instant nextAttemptAt,
            Instant leaseExpiresAt,
            Instant startedAt,
            Instant completedAt,
            Instant failedAt,
            String lastErrorCode,
            String lastErrorMessage,
            Instant createdAt,
            Instant updatedAt) {
        this(processingId, AssetProcessingTarget.localization(contentId, languageCode), contentType, externalKey,
                coverSourceAssetId, audioSourceAssetId, pageCount, status, attemptCount, nextAttemptAt,
                leaseExpiresAt, startedAt, completedAt, failedAt, lastErrorCode, lastErrorMessage, createdAt, updatedAt);
    }

    public AssetProcessingRecord {
        if (processingId == null || processingId <= 0) {
            throw new IllegalArgumentException("Processing ID must be positive");
        }
        if (target == null) {
            throw new IllegalArgumentException("Asset processing target must not be null");
        }
        if (contentType == null) {
            throw new IllegalArgumentException("Processing content type must not be null");
        }
        if (externalKey == null || externalKey.isBlank()) {
            throw new IllegalArgumentException("External key must not be blank");
        }
        if (status == null) {
            throw new IllegalArgumentException("Processing status must not be null");
        }
        if (attemptCount < 0) {
            throw new IllegalArgumentException("Attempt count must not be negative");
        }
        if (pageCount != null && pageCount < 0) {
            throw new IllegalArgumentException("Page count must not be negative");
        }
        if (nextAttemptAt == null) {
            throw new IllegalArgumentException("Next attempt time must not be null");
        }
        if (createdAt == null || updatedAt == null) {
            throw new IllegalArgumentException("Audit timestamps must not be null");
        }
    }

    public Long contentId() { return target.contentId(); }

    public LanguageCode languageCode() { return target.languageCode(); }

    public AssetProcessingTargetScope targetScope() { return target.scope(); }
}
