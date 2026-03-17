package com.tellpal.v2.asset.api;

import java.time.Instant;

public record AssetRecord(
        Long assetId,
        AssetStorageLocation storageLocation,
        AssetMediaType mediaType,
        AssetKind kind,
        String mimeType,
        Long byteSize,
        String checksumSha256,
        String cachedDownloadUrl,
        Instant downloadUrlCachedAt,
        Instant downloadUrlExpiresAt,
        Instant createdAt,
        Instant updatedAt) {

    public AssetRecord {
        if (assetId == null || assetId <= 0) {
            throw new IllegalArgumentException("Asset ID must be positive");
        }
        if (storageLocation == null) {
            throw new IllegalArgumentException("Asset storage location must not be null");
        }
        if (mediaType == null) {
            throw new IllegalArgumentException("Asset media type must not be null");
        }
        if (kind == null) {
            throw new IllegalArgumentException("Asset kind must not be null");
        }
        if (createdAt == null || updatedAt == null) {
            throw new IllegalArgumentException("Asset audit timestamps must not be null");
        }
    }
}
