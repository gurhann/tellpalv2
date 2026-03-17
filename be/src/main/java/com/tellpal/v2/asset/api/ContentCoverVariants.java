package com.tellpal.v2.asset.api;

public record ContentCoverVariants(
        ResolvedAssetReference thumbnailPhone,
        ResolvedAssetReference thumbnailTablet,
        ResolvedAssetReference detailPhone,
        ResolvedAssetReference detailTablet) {

    public static ContentCoverVariants empty() {
        return new ContentCoverVariants(null, null, null, null);
    }
}
