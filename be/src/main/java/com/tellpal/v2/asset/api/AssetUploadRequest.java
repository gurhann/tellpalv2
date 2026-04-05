package com.tellpal.v2.asset.api;

import java.time.Instant;
import java.util.Map;

/**
 * Signed direct-upload request returned to the CMS for one asset upload.
 */
public record AssetUploadRequest(
        AssetStorageProvider provider,
        String objectPath,
        String uploadUrl,
        String httpMethod,
        Map<String, String> requiredHeaders,
        Instant expiresAt,
        String uploadToken) {

    public AssetUploadRequest {
        if (provider == null) {
            throw new IllegalArgumentException("Asset upload provider must not be null");
        }
        if (objectPath == null || objectPath.isBlank()) {
            throw new IllegalArgumentException("Asset upload object path must not be blank");
        }
        if (uploadUrl == null || uploadUrl.isBlank()) {
            throw new IllegalArgumentException("Asset upload URL must not be blank");
        }
        if (httpMethod == null || httpMethod.isBlank()) {
            throw new IllegalArgumentException("Asset upload HTTP method must not be blank");
        }
        if (requiredHeaders == null) {
            throw new IllegalArgumentException("Asset upload headers must not be null");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("Asset upload expiry must not be null");
        }
        if (uploadToken == null || uploadToken.isBlank()) {
            throw new IllegalArgumentException("Asset upload token must not be blank");
        }
        objectPath = objectPath.trim();
        uploadUrl = uploadUrl.trim();
        httpMethod = httpMethod.trim();
        requiredHeaders = Map.copyOf(requiredHeaders);
        uploadToken = uploadToken.trim();
    }
}
