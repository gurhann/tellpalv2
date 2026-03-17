package com.tellpal.v2.category.web.mobile;

import java.time.Instant;

import com.tellpal.v2.asset.api.ContentCoverVariants;
import com.tellpal.v2.asset.api.ContentDeliveryAssets;
import com.tellpal.v2.asset.api.ContentPackageAssets;
import com.tellpal.v2.asset.api.ResolvedAssetReference;
import com.tellpal.v2.category.api.PublicCategoryView;
import com.tellpal.v2.content.api.PublicContentSummary;

record MobileCategoryResponse(
        Long categoryId,
        String type,
        String slug,
        String languageCode,
        String name,
        String description,
        boolean premium,
        MobileCategoryAssetResponse image) {

    static MobileCategoryResponse from(PublicCategoryView category) {
        return new MobileCategoryResponse(
                category.categoryId(),
                category.type().name(),
                category.slug(),
                category.languageCode().value(),
                category.name(),
                category.description(),
                category.premium(),
                MobileCategoryAssetResponse.from(category.image()));
    }
}

record MobileCategoryContentResponse(
        Long contentId,
        String type,
        String externalKey,
        String languageCode,
        String title,
        String description,
        Integer ageRange,
        Integer pageCount,
        Integer durationMinutes,
        boolean isFree,
        MobileCategoryContentAssetsResponse assets) {

    static MobileCategoryContentResponse from(PublicContentSummary summary) {
        return new MobileCategoryContentResponse(
                summary.contentId(),
                summary.type().name(),
                summary.externalKey(),
                summary.languageCode().value(),
                summary.title(),
                summary.description(),
                summary.ageRange(),
                summary.pageCount(),
                summary.durationMinutes(),
                summary.free(),
                MobileCategoryContentAssetsResponse.from(summary.assets()));
    }
}

record MobileCategoryContentAssetsResponse(
        MobileCategoryCoverVariantsResponse coverVariants,
        MobileCategoryPackageAssetsResponse packages,
        MobileCategoryAssetResponse optimizedAudio) {

    static MobileCategoryContentAssetsResponse from(ContentDeliveryAssets assets) {
        ContentDeliveryAssets safeAssets = assets == null ? ContentDeliveryAssets.empty() : assets;
        return new MobileCategoryContentAssetsResponse(
                MobileCategoryCoverVariantsResponse.from(safeAssets.coverVariants()),
                MobileCategoryPackageAssetsResponse.from(safeAssets.packages()),
                MobileCategoryAssetResponse.from(safeAssets.optimizedAudio()));
    }
}

record MobileCategoryCoverVariantsResponse(
        MobileCategoryAssetResponse thumbnailPhone,
        MobileCategoryAssetResponse thumbnailTablet,
        MobileCategoryAssetResponse detailPhone,
        MobileCategoryAssetResponse detailTablet) {

    static MobileCategoryCoverVariantsResponse from(ContentCoverVariants coverVariants) {
        ContentCoverVariants safeCoverVariants = coverVariants == null ? ContentCoverVariants.empty() : coverVariants;
        return new MobileCategoryCoverVariantsResponse(
                MobileCategoryAssetResponse.from(safeCoverVariants.thumbnailPhone()),
                MobileCategoryAssetResponse.from(safeCoverVariants.thumbnailTablet()),
                MobileCategoryAssetResponse.from(safeCoverVariants.detailPhone()),
                MobileCategoryAssetResponse.from(safeCoverVariants.detailTablet()));
    }
}

record MobileCategoryPackageAssetsResponse(
        MobileCategoryAssetResponse singlePackage,
        MobileCategoryAssetResponse storyPart1,
        MobileCategoryAssetResponse storyPart2) {

    static MobileCategoryPackageAssetsResponse from(ContentPackageAssets packages) {
        ContentPackageAssets safePackages = packages == null ? ContentPackageAssets.empty() : packages;
        return new MobileCategoryPackageAssetsResponse(
                MobileCategoryAssetResponse.from(safePackages.singlePackage()),
                MobileCategoryAssetResponse.from(safePackages.storyPart1()),
                MobileCategoryAssetResponse.from(safePackages.storyPart2()));
    }
}

record MobileCategoryAssetResponse(
        Long assetId,
        String kind,
        String provider,
        String objectPath,
        String mimeType,
        String downloadUrl,
        Instant downloadUrlExpiresAt) {

    static MobileCategoryAssetResponse from(ResolvedAssetReference assetReference) {
        if (assetReference == null) {
            return null;
        }
        return new MobileCategoryAssetResponse(
                assetReference.assetId(),
                assetReference.kind().name(),
                assetReference.provider().name(),
                assetReference.objectPath(),
                assetReference.mimeType(),
                assetReference.downloadUrl(),
                assetReference.downloadUrlExpiresAt());
    }
}
