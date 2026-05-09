package com.tellpal.v2.asset.api;

import java.io.InputStream;

/**
 * Backend-owned upload command for CMS media files.
 */
public record BackendMediaAssetUploadCommand(
        AssetKind kind,
        String fileName,
        String mimeType,
        long byteSize,
        InputStream content,
        String checksumSha256) {

    public BackendMediaAssetUploadCommand {
        if (kind == null) {
            throw new IllegalArgumentException("Upload kind must not be null");
        }
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("Upload file name must not be blank");
        }
        if (mimeType == null || mimeType.isBlank()) {
            throw new IllegalArgumentException("Upload MIME type must not be blank");
        }
        if (byteSize <= 0) {
            throw new IllegalArgumentException("Upload byte size must be positive");
        }
        if (content == null) {
            throw new IllegalArgumentException("Upload content stream must not be null");
        }
        fileName = fileName.trim();
        mimeType = mimeType.trim();
    }
}
