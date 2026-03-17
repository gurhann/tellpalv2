package com.tellpal.v2.asset.api;

public record UpdateMediaAssetMetadataCommand(
        Long assetId,
        String mimeType,
        Long byteSize,
        String checksumSha256) {

    public UpdateMediaAssetMetadataCommand {
        if (assetId == null || assetId <= 0) {
            throw new IllegalArgumentException("Asset ID must be positive");
        }
        if (byteSize != null && byteSize < 0) {
            throw new IllegalArgumentException("Asset byte size must not be negative");
        }
    }
}
