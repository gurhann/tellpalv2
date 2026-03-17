package com.tellpal.v2.asset.api;

public record RefreshMediaAssetDownloadUrlCommand(Long assetId) {

    public RefreshMediaAssetDownloadUrlCommand {
        if (assetId == null || assetId <= 0) {
            throw new IllegalArgumentException("Asset ID must be positive");
        }
    }
}
