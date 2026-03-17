package com.tellpal.v2.content.web.mobile;

import java.time.Instant;

import com.tellpal.v2.asset.api.ContentCoverVariants;
import com.tellpal.v2.asset.api.ContentDeliveryAssets;
import com.tellpal.v2.asset.api.ContentPackageAssets;
import com.tellpal.v2.asset.api.ResolvedAssetReference;
import com.tellpal.v2.content.api.PublicContentDetails;
import com.tellpal.v2.content.api.PublicContentSummary;
import com.tellpal.v2.content.api.PublicStoryPage;

record MobileContentSummaryResponse(
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
        MobileContentAssetsResponse assets) {

    static MobileContentSummaryResponse from(PublicContentSummary summary) {
        return new MobileContentSummaryResponse(
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
                MobileContentAssetsResponse.from(summary.assets()));
    }
}

record MobileContentDetailsResponse(
        Long contentId,
        String type,
        String externalKey,
        String languageCode,
        String title,
        String description,
        String bodyText,
        Integer ageRange,
        Integer pageCount,
        Integer durationMinutes,
        Instant publishedAt,
        boolean isFree,
        MobileContentAssetsResponse assets) {

    static MobileContentDetailsResponse from(PublicContentDetails details) {
        return new MobileContentDetailsResponse(
                details.contentId(),
                details.type().name(),
                details.externalKey(),
                details.languageCode().value(),
                details.title(),
                details.description(),
                details.bodyText(),
                details.ageRange(),
                details.pageCount(),
                details.durationMinutes(),
                details.publishedAt(),
                details.free(),
                MobileContentAssetsResponse.from(details.assets()));
    }
}

record MobileStoryPageResponse(
        int pageNumber,
        String languageCode,
        String bodyText,
        MobileAssetResponse illustration,
        MobileAssetResponse audio) {

    static MobileStoryPageResponse from(PublicStoryPage storyPage) {
        return new MobileStoryPageResponse(
                storyPage.pageNumber(),
                storyPage.languageCode().value(),
                storyPage.bodyText(),
                MobileAssetResponse.from(storyPage.illustration()),
                MobileAssetResponse.from(storyPage.audio()));
    }
}

record MobileContentAssetsResponse(
        MobileCoverVariantsResponse coverVariants,
        MobilePackageAssetsResponse packages,
        MobileAssetResponse optimizedAudio) {

    static MobileContentAssetsResponse from(ContentDeliveryAssets assets) {
        ContentDeliveryAssets safeAssets = assets == null ? ContentDeliveryAssets.empty() : assets;
        return new MobileContentAssetsResponse(
                MobileCoverVariantsResponse.from(safeAssets.coverVariants()),
                MobilePackageAssetsResponse.from(safeAssets.packages()),
                MobileAssetResponse.from(safeAssets.optimizedAudio()));
    }
}

record MobileCoverVariantsResponse(
        MobileAssetResponse thumbnailPhone,
        MobileAssetResponse thumbnailTablet,
        MobileAssetResponse detailPhone,
        MobileAssetResponse detailTablet) {

    static MobileCoverVariantsResponse from(ContentCoverVariants coverVariants) {
        ContentCoverVariants safeCoverVariants = coverVariants == null ? ContentCoverVariants.empty() : coverVariants;
        return new MobileCoverVariantsResponse(
                MobileAssetResponse.from(safeCoverVariants.thumbnailPhone()),
                MobileAssetResponse.from(safeCoverVariants.thumbnailTablet()),
                MobileAssetResponse.from(safeCoverVariants.detailPhone()),
                MobileAssetResponse.from(safeCoverVariants.detailTablet()));
    }
}

record MobilePackageAssetsResponse(
        MobileAssetResponse singlePackage,
        MobileAssetResponse storyPart1,
        MobileAssetResponse storyPart2) {

    static MobilePackageAssetsResponse from(ContentPackageAssets packages) {
        ContentPackageAssets safePackages = packages == null ? ContentPackageAssets.empty() : packages;
        return new MobilePackageAssetsResponse(
                MobileAssetResponse.from(safePackages.singlePackage()),
                MobileAssetResponse.from(safePackages.storyPart1()),
                MobileAssetResponse.from(safePackages.storyPart2()));
    }
}

record MobileAssetResponse(
        Long assetId,
        String kind,
        String provider,
        String objectPath,
        String mimeType,
        String downloadUrl,
        Instant downloadUrlExpiresAt) {

    static MobileAssetResponse from(ResolvedAssetReference assetReference) {
        if (assetReference == null) {
            return null;
        }
        return new MobileAssetResponse(
                assetReference.assetId(),
                assetReference.kind().name(),
                assetReference.provider().name(),
                assetReference.objectPath(),
                assetReference.mimeType(),
                assetReference.downloadUrl(),
                assetReference.downloadUrlExpiresAt());
    }
}
