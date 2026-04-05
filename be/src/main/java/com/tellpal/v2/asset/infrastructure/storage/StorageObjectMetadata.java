package com.tellpal.v2.asset.infrastructure.storage;

/**
 * Observed storage metadata for one uploaded object.
 */
public record StorageObjectMetadata(String mimeType, Long byteSize) {

    public StorageObjectMetadata {
        if (byteSize != null && byteSize < 0) {
            throw new IllegalArgumentException("Object byte size must not be negative");
        }
        mimeType = mimeType == null || mimeType.isBlank() ? null : mimeType.trim();
    }
}
