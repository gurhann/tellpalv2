package com.tellpal.v2.asset.infrastructure.storage;

import java.time.Instant;

public record StorageSignedDownloadUrl(String url, Instant expiresAt) {

    public StorageSignedDownloadUrl {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Signed download URL must not be blank");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("Signed download URL expiry must not be null");
        }
        url = url.trim();
    }
}
