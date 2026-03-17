package com.tellpal.v2.asset.api;

import java.time.Instant;

public record ResolvedAssetReference(
        Long assetId,
        AssetKind kind,
        AssetStorageProvider provider,
        String objectPath,
        String mimeType,
        String downloadUrl,
        Instant downloadUrlExpiresAt) {

    public ResolvedAssetReference {
        if (assetId == null || assetId <= 0) {
            throw new IllegalArgumentException("Asset ID must be positive");
        }
        if (kind == null) {
            throw new IllegalArgumentException("Asset kind must not be null");
        }
        if (provider == null) {
            throw new IllegalArgumentException("Asset storage provider must not be null");
        }
        if (objectPath == null || objectPath.isBlank()) {
            throw new IllegalArgumentException("Asset object path must not be blank");
        }
        objectPath = objectPath.trim();
    }
}
