package com.tellpal.v2.asset.api;

import java.io.InputStream;

/**
 * Fallback upload command for networks that cannot reach Google Storage directly.
 */
public record ProxyMediaAssetUploadCommand(
        String uploadToken,
        String mimeType,
        long byteSize,
        InputStream content,
        String checksumSha256) {

    public ProxyMediaAssetUploadCommand {
        if (uploadToken == null || uploadToken.isBlank()) {
            throw new IllegalArgumentException("Upload token must not be blank");
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
        uploadToken = uploadToken.trim();
        mimeType = mimeType.trim();
    }
}
