package com.tellpal.v2.asset.api;

/** Ordered localized source references captured for an immutable story-page media revision. */
public record AssetProcessingPageSource(
        Long pageId,
        int pageNumber,
        Long illustrationAssetId,
        Long audioAssetId) {

    public AssetProcessingPageSource {
        if (pageId == null || pageId <= 0 || pageNumber <= 0) {
            throw new IllegalArgumentException("Story page identity must be positive");
        }
        if (illustrationAssetId != null && illustrationAssetId <= 0) {
            throw new IllegalArgumentException("Illustration asset ID must be positive");
        }
        if (audioAssetId != null && audioAssetId <= 0) {
            throw new IllegalArgumentException("Audio asset ID must be positive");
        }
    }
}
