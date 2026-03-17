package com.tellpal.v2.asset.api;

import java.time.Instant;

import com.tellpal.v2.shared.domain.LanguageCode;

public record AssetProcessingRecord(
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

    public AssetProcessingRecord {
        if (processingId == null || processingId <= 0) {
            throw new IllegalArgumentException("Processing ID must be positive");
        }
        if (contentId == null || contentId <= 0) {
            throw new IllegalArgumentException("Content ID must be positive");
        }
        if (languageCode == null) {
            throw new IllegalArgumentException("Language code must not be null");
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
}
