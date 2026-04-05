package com.tellpal.v2.asset.api;

/**
 * Command for finalizing a direct browser upload after the object reaches storage.
 */
public record CompleteMediaAssetUploadCommand(
        String uploadToken,
        String checksumSha256) {

    public CompleteMediaAssetUploadCommand {
        if (uploadToken == null || uploadToken.isBlank()) {
            throw new IllegalArgumentException("Asset upload token must not be blank");
        }
        uploadToken = uploadToken.trim();
    }
}
