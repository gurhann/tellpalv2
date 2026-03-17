package com.tellpal.v2.asset.api;

public record ContentDeliveryAssets(
        ContentCoverVariants coverVariants,
        ContentPackageAssets packages,
        ResolvedAssetReference optimizedAudio) {

    public ContentDeliveryAssets {
        coverVariants = coverVariants == null ? ContentCoverVariants.empty() : coverVariants;
        packages = packages == null ? ContentPackageAssets.empty() : packages;
    }

    public static ContentDeliveryAssets empty() {
        return new ContentDeliveryAssets(ContentCoverVariants.empty(), ContentPackageAssets.empty(), null);
    }
}
