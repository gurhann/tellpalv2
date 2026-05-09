package com.tellpal.v2.asset.infrastructure.storage;

import java.io.InputStream;

public record StorageObjectContent(
        String mimeType,
        long byteSize,
        long contentLength,
        Long rangeStartInclusive,
        Long rangeEndInclusive,
        InputStream content) {

    public StorageObjectContent {
        if (mimeType == null || mimeType.isBlank()) {
            throw new IllegalArgumentException("Storage object MIME type must not be blank");
        }
        if (byteSize < 0) {
            throw new IllegalArgumentException("Storage object byte size must not be negative");
        }
        if (contentLength < 0) {
            throw new IllegalArgumentException("Storage object content length must not be negative");
        }
        if (content == null) {
            throw new IllegalArgumentException("Storage object content stream must not be null");
        }
        mimeType = mimeType.trim();
    }
}
