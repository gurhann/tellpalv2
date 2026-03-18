package com.tellpal.v2.asset.api;

/**
 * Delivery-oriented view of the generated assets for a content localization.
 *
 * <p>Missing groups are normalized to empty value objects so callers can handle partial availability
 * without null checks on the containers themselves.
 */
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
