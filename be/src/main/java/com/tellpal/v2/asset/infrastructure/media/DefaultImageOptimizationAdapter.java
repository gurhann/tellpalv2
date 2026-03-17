package com.tellpal.v2.asset.infrastructure.media;

import java.util.List;

import org.springframework.stereotype.Component;

import com.tellpal.v2.asset.api.AssetKind;
import com.tellpal.v2.asset.api.AssetProcessingRecord;
import com.tellpal.v2.asset.infrastructure.storage.AssetProcessingPathBuilder;

@Component
class DefaultImageOptimizationAdapter implements ImageOptimizationAdapter {

    private final AssetProcessingPathBuilder pathBuilder;

    DefaultImageOptimizationAdapter(AssetProcessingPathBuilder pathBuilder) {
        this.pathBuilder = pathBuilder;
    }

    @Override
    public List<GeneratedAssetPlan> generateCoverVariants(AssetProcessingRecord assetProcessingRecord) {
        return List.of(
                new GeneratedAssetPlan(
                        AssetKind.THUMBNAIL_PHONE,
                        pathBuilder.coverVariantPath(assetProcessingRecord, AssetKind.THUMBNAIL_PHONE),
                        "image/webp"),
                new GeneratedAssetPlan(
                        AssetKind.THUMBNAIL_TABLET,
                        pathBuilder.coverVariantPath(assetProcessingRecord, AssetKind.THUMBNAIL_TABLET),
                        "image/webp"),
                new GeneratedAssetPlan(
                        AssetKind.DETAIL_PHONE,
                        pathBuilder.coverVariantPath(assetProcessingRecord, AssetKind.DETAIL_PHONE),
                        "image/webp"),
                new GeneratedAssetPlan(
                        AssetKind.DETAIL_TABLET,
                        pathBuilder.coverVariantPath(assetProcessingRecord, AssetKind.DETAIL_TABLET),
                        "image/webp"));
    }
}
