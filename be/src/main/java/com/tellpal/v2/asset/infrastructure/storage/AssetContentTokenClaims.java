package com.tellpal.v2.asset.infrastructure.storage;

import java.time.Instant;

import com.tellpal.v2.asset.domain.StorageProvider;

public record AssetContentTokenClaims(
        Long assetId,
        StorageProvider provider,
        String objectPath,
        Instant issuedAt,
        Instant expiresAt) {

    public AssetContentTokenClaims {
        if (assetId == null || assetId <= 0) {
            throw new IllegalArgumentException("Content token asset ID must be positive");
        }
        if (provider == null) {
            throw new IllegalArgumentException("Content token provider must not be null");
        }
        if (objectPath == null || objectPath.isBlank()) {
            throw new IllegalArgumentException("Content token object path must not be blank");
        }
        if (issuedAt == null || expiresAt == null) {
            throw new IllegalArgumentException("Content token timestamps must not be null");
        }
        if (!expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException("Content token expiry must be after issue time");
        }
        objectPath = objectPath.trim();
    }
}
