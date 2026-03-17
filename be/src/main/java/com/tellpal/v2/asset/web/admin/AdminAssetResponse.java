package com.tellpal.v2.asset.web.admin;

import java.time.Instant;

import com.tellpal.v2.asset.api.AssetRecord;

public record AdminAssetResponse(
        Long assetId,
        String provider,
        String objectPath,
        String mediaType,
        String kind,
        String mimeType,
        Long byteSize,
        String checksumSha256,
        String cachedDownloadUrl,
        Instant downloadUrlCachedAt,
        Instant downloadUrlExpiresAt,
        Instant createdAt,
        Instant updatedAt) {

    static AdminAssetResponse from(AssetRecord record) {
        return new AdminAssetResponse(
                record.assetId(),
                record.storageLocation().provider().name(),
                record.storageLocation().objectPath(),
                record.mediaType().name(),
                record.kind().name(),
                record.mimeType(),
                record.byteSize(),
                record.checksumSha256(),
                record.cachedDownloadUrl(),
                record.downloadUrlCachedAt(),
                record.downloadUrlExpiresAt(),
                record.createdAt(),
                record.updatedAt());
    }
}
