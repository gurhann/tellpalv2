package com.tellpal.v2.asset.api;

public record RegisterMediaAssetCommand(
        AssetStorageProvider provider,
        String objectPath,
        AssetKind kind,
        String mimeType,
        Long byteSize,
        String checksumSha256) {

    public RegisterMediaAssetCommand {
        if (provider == null) {
            throw new IllegalArgumentException("Asset storage provider must not be null");
        }
        if (objectPath == null || objectPath.isBlank()) {
            throw new IllegalArgumentException("Asset object path must not be blank");
        }
        if (kind == null) {
            throw new IllegalArgumentException("Asset kind must not be null");
        }
        if (byteSize != null && byteSize < 0) {
            throw new IllegalArgumentException("Asset byte size must not be negative");
        }
        objectPath = objectPath.trim();
    }
}
