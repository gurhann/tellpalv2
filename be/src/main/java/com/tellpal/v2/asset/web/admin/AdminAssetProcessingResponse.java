package com.tellpal.v2.asset.web.admin;

import java.time.Instant;

import com.tellpal.v2.asset.api.AssetProcessingRecord;
import com.tellpal.v2.asset.api.AssetProcessingContentType;
import com.tellpal.v2.asset.api.AssetProcessingState;
import com.tellpal.v2.shared.domain.LanguageCode;

record AdminAssetProcessingResponse(
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

    static AdminAssetProcessingResponse from(AssetProcessingRecord assetProcessingRecord) {
        return new AdminAssetProcessingResponse(
                assetProcessingRecord.processingId(),
                assetProcessingRecord.contentId(),
                assetProcessingRecord.languageCode(),
                assetProcessingRecord.contentType(),
                assetProcessingRecord.externalKey(),
                assetProcessingRecord.coverSourceAssetId(),
                assetProcessingRecord.audioSourceAssetId(),
                assetProcessingRecord.pageCount(),
                assetProcessingRecord.status(),
                assetProcessingRecord.attemptCount(),
                assetProcessingRecord.nextAttemptAt(),
                assetProcessingRecord.leaseExpiresAt(),
                assetProcessingRecord.startedAt(),
                assetProcessingRecord.completedAt(),
                assetProcessingRecord.failedAt(),
                assetProcessingRecord.lastErrorCode(),
                assetProcessingRecord.lastErrorMessage(),
                assetProcessingRecord.createdAt(),
                assetProcessingRecord.updatedAt());
    }
}
