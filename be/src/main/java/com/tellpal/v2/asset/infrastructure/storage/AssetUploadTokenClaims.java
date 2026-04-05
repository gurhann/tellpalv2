package com.tellpal.v2.asset.infrastructure.storage;

import java.time.Instant;

import com.tellpal.v2.asset.domain.MediaKind;
import com.tellpal.v2.asset.domain.StorageProvider;

public record AssetUploadTokenClaims(
        StorageProvider provider,
        String objectPath,
        MediaKind kind,
        String mimeType,
        long byteSize,
        Instant issuedAt,
        Instant expiresAt) {

    public AssetUploadTokenClaims {
        if (provider == null) {
            throw new IllegalArgumentException("Upload token provider must not be null");
        }
        if (objectPath == null || objectPath.isBlank()) {
            throw new IllegalArgumentException("Upload token object path must not be blank");
        }
        if (kind == null) {
            throw new IllegalArgumentException("Upload token asset kind must not be null");
        }
        if (mimeType == null || mimeType.isBlank()) {
            throw new IllegalArgumentException("Upload token MIME type must not be blank");
        }
        if (byteSize <= 0) {
            throw new IllegalArgumentException("Upload token byte size must be positive");
        }
        if (issuedAt == null || expiresAt == null) {
            throw new IllegalArgumentException("Upload token timestamps must not be null");
        }
        if (!expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException("Upload token expiry must be after issue time");
        }
        objectPath = objectPath.trim();
        mimeType = mimeType.trim();
    }
}
