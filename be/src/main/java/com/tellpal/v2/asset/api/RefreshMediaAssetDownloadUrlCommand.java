package com.tellpal.v2.asset.api;

/**
 * Command for replacing the cached signed download URL of an existing media asset.
 */
public record RefreshMediaAssetDownloadUrlCommand(Long assetId) {

    public RefreshMediaAssetDownloadUrlCommand {
        if (assetId == null || assetId <= 0) {
            throw new IllegalArgumentException("Asset ID must be positive");
        }
    }
}
