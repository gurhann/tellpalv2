package com.tellpal.v2.asset.infrastructure.storage;

public record StorageObjectRange(
        long startInclusive,
        long endInclusive) {

    public StorageObjectRange {
        if (startInclusive < 0) {
            throw new IllegalArgumentException("Storage object range start must not be negative");
        }
        if (endInclusive < startInclusive) {
            throw new IllegalArgumentException("Storage object range end must not be before start");
        }
    }

    public long contentLength() {
        return endInclusive - startInclusive + 1;
    }
}
