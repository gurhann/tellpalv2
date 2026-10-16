package com.tellpal.v2.asset.infrastructure.storage;

/** Storage-owned immutable generation evidence, separate from mutable registry metadata. */
public record VersionedStorageObject(String path, String version, String mime, long bytes) {
    public VersionedStorageObject {
        if (path == null || path.isBlank() || version == null || version.isBlank() || mime == null || bytes <= 0)
            throw new IllegalArgumentException("Positive versioned object evidence required");
    }
}
