package com.tellpal.v2.asset.infrastructure.storage;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalized Firebase Storage settings owned by the asset module.
 */
@ConfigurationProperties("tellpal.asset.storage.firebase")
public record AssetStorageFirebaseProperties(
        boolean fakeClientEnabled,
        String projectId,
        String bucketName,
        String credentialsPath,
        String pathPrefix,
        Duration signedUploadTtl,
        Duration signedDownloadTtl) {

    public AssetStorageFirebaseProperties {
        signedUploadTtl = signedUploadTtl == null ? Duration.ofMinutes(15) : signedUploadTtl;
        signedDownloadTtl = signedDownloadTtl == null ? Duration.ofMinutes(15) : signedDownloadTtl;
    }
}
