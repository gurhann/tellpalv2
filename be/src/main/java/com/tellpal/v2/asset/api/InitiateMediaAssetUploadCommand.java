package com.tellpal.v2.asset.api;

/**
 * Command for preparing a direct browser upload into Firebase Storage.
 */
public record InitiateMediaAssetUploadCommand(
        AssetKind kind,
        String fileName,
        String mimeType,
        Long byteSize) {

    public InitiateMediaAssetUploadCommand {
        if (kind == null) {
            throw new IllegalArgumentException("Asset upload kind must not be null");
        }
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("Asset upload file name must not be blank");
        }
        if (mimeType == null || mimeType.isBlank()) {
            throw new IllegalArgumentException("Asset upload MIME type must not be blank");
        }
        if (byteSize == null || byteSize <= 0) {
            throw new IllegalArgumentException("Asset upload byte size must be positive");
        }
        fileName = fileName.trim();
        mimeType = mimeType.trim();
    }
}
