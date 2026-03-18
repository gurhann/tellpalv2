package com.tellpal.v2.asset.api;

/**
 * Generated downloadable content packages for a localization.
 */
public record ContentPackageAssets(
        ResolvedAssetReference singlePackage,
        ResolvedAssetReference storyPart1,
        ResolvedAssetReference storyPart2) {

    public static ContentPackageAssets empty() {
        return new ContentPackageAssets(null, null, null);
    }
}
