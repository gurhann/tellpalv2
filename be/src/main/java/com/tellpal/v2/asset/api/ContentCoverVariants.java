package com.tellpal.v2.asset.api;

/**
 * Generated cover image variants grouped by target surface.
 */
public record ContentCoverVariants(
        ResolvedAssetReference thumbnailPhone,
        ResolvedAssetReference thumbnailTablet,
        ResolvedAssetReference detailPhone,
        ResolvedAssetReference detailTablet) {

    public static ContentCoverVariants empty() {
        return new ContentCoverVariants(null, null, null, null);
    }
}
