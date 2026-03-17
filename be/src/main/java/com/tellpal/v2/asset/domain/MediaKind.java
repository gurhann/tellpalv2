package com.tellpal.v2.asset.domain;

public enum MediaKind {

    ORIGINAL_IMAGE(MediaAssetType.IMAGE),
    ORIGINAL_AUDIO(MediaAssetType.AUDIO),
    THUMBNAIL_PHONE(MediaAssetType.IMAGE),
    THUMBNAIL_TABLET(MediaAssetType.IMAGE),
    DETAIL_PHONE(MediaAssetType.IMAGE),
    DETAIL_TABLET(MediaAssetType.IMAGE),
    OPTIMIZED_AUDIO(MediaAssetType.AUDIO),
    CONTENT_ZIP(MediaAssetType.ARCHIVE),
    CONTENT_ZIP_PART1(MediaAssetType.ARCHIVE),
    CONTENT_ZIP_PART2(MediaAssetType.ARCHIVE);

    private final MediaAssetType mediaType;

    MediaKind(MediaAssetType mediaType) {
        this.mediaType = mediaType;
    }

    public MediaAssetType getMediaType() {
        return mediaType;
    }

    public boolean belongsTo(MediaAssetType mediaAssetType) {
        return mediaType == mediaAssetType;
    }
}
