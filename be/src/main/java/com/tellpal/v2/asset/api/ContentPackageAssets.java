package com.tellpal.v2.asset.api;

public record ContentPackageAssets(
        ResolvedAssetReference singlePackage,
        ResolvedAssetReference storyPart1,
        ResolvedAssetReference storyPart2) {

    public static ContentPackageAssets empty() {
        return new ContentPackageAssets(null, null, null);
    }
}
