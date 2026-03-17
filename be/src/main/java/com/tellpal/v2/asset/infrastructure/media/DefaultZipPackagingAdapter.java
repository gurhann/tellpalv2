package com.tellpal.v2.asset.infrastructure.media;

import java.util.List;

import org.springframework.stereotype.Component;

import com.tellpal.v2.asset.api.AssetKind;
import com.tellpal.v2.asset.api.AssetProcessingContentType;
import com.tellpal.v2.asset.api.AssetProcessingRecord;
import com.tellpal.v2.asset.infrastructure.storage.AssetProcessingPathBuilder;

@Component
class DefaultZipPackagingAdapter implements ZipPackagingAdapter {

    private final AssetProcessingPathBuilder pathBuilder;

    DefaultZipPackagingAdapter(AssetProcessingPathBuilder pathBuilder) {
        this.pathBuilder = pathBuilder;
    }

    @Override
    public List<GeneratedAssetPlan> generatePackages(AssetProcessingRecord assetProcessingRecord) {
        if (assetProcessingRecord.contentType() == AssetProcessingContentType.STORY) {
            return List.of(
                    new GeneratedAssetPlan(
                            AssetKind.CONTENT_ZIP_PART1,
                            pathBuilder.packagePath(assetProcessingRecord, AssetKind.CONTENT_ZIP_PART1),
                            "application/zip"),
                    new GeneratedAssetPlan(
                            AssetKind.CONTENT_ZIP_PART2,
                            pathBuilder.packagePath(assetProcessingRecord, AssetKind.CONTENT_ZIP_PART2),
                            "application/zip"));
        }
        return List.of(new GeneratedAssetPlan(
                AssetKind.CONTENT_ZIP,
                pathBuilder.packagePath(assetProcessingRecord, AssetKind.CONTENT_ZIP),
                "application/zip"));
    }
}
