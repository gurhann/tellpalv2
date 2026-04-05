package com.tellpal.v2.asset.infrastructure.storage;

import java.time.Instant;
import java.util.Map;

/**
 * Signed upload request data returned to the CMS for a direct browser upload.
 */
public record StorageSignedUploadUrl(
        String url,
        String httpMethod,
        Map<String, String> requiredHeaders,
        Instant expiresAt) {

    public StorageSignedUploadUrl {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Signed upload URL must not be blank");
        }
        if (httpMethod == null || httpMethod.isBlank()) {
            throw new IllegalArgumentException("Signed upload HTTP method must not be blank");
        }
        if (requiredHeaders == null) {
            throw new IllegalArgumentException("Signed upload headers must not be null");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("Signed upload URL expiry must not be null");
        }
        url = url.trim();
        httpMethod = httpMethod.trim();
        requiredHeaders = Map.copyOf(requiredHeaders);
    }
}
