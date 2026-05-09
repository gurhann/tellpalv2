package com.tellpal.v2.asset.api;

import java.io.InputStream;

/**
 * Streamable content response for a registered media asset.
 */
public record AssetContent(
        Long assetId,
        String fileName,
        String mimeType,
        long byteSize,
        long contentLength,
        Long rangeStartInclusive,
        Long rangeEndInclusive,
        InputStream content) {

    public AssetContent {
        if (assetId == null || assetId <= 0) {
            throw new IllegalArgumentException("Asset ID must be positive");
        }
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("Asset content file name must not be blank");
        }
        if (mimeType == null || mimeType.isBlank()) {
            throw new IllegalArgumentException("Asset content MIME type must not be blank");
        }
        if (byteSize < 0) {
            throw new IllegalArgumentException("Asset content byte size must not be negative");
        }
        if (contentLength < 0) {
            throw new IllegalArgumentException("Asset content length must not be negative");
        }
        if (content == null) {
            throw new IllegalArgumentException("Asset content stream must not be null");
        }
        fileName = fileName.trim();
        mimeType = mimeType.trim();
    }

    public boolean isPartial() {
        return rangeStartInclusive != null && rangeEndInclusive != null;
    }
}
