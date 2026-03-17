package com.tellpal.v2.asset.application;

import com.tellpal.v2.asset.api.AssetProcessingRecord;
import com.tellpal.v2.asset.api.AssetProcessingState;
import com.tellpal.v2.asset.domain.AssetProcessing;

final class AssetProcessingMapper {

    private AssetProcessingMapper() {
    }

    static AssetProcessingRecord toRecord(AssetProcessing assetProcessing) {
        Long processingId = assetProcessing.getId();
        if (processingId == null || processingId <= 0) {
            throw new IllegalStateException("Asset processing must be persisted before mapping to API");
        }
        return new AssetProcessingRecord(
                processingId,
                assetProcessing.getContentId(),
                assetProcessing.getLanguageCode(),
                AssetProcessingState.valueOf(assetProcessing.getStatus().name()),
                assetProcessing.getAttemptCount(),
                assetProcessing.getNextAttemptAt(),
                assetProcessing.getLeaseExpiresAt(),
                assetProcessing.getStartedAt(),
                assetProcessing.getCompletedAt(),
                assetProcessing.getFailedAt(),
                assetProcessing.getLastErrorCode(),
                assetProcessing.getLastErrorMessage(),
                assetProcessing.getCreatedAt(),
                assetProcessing.getUpdatedAt());
    }
}
