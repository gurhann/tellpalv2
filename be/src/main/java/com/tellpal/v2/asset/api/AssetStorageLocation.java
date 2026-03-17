package com.tellpal.v2.asset.api;

public record AssetStorageLocation(AssetStorageProvider provider, String objectPath) {

    public AssetStorageLocation {
        if (provider == null) {
            throw new IllegalArgumentException("Asset storage provider must not be null");
        }
        if (objectPath == null || objectPath.isBlank()) {
            throw new IllegalArgumentException("Asset object path must not be blank");
        }
        objectPath = objectPath.trim();
    }
}
