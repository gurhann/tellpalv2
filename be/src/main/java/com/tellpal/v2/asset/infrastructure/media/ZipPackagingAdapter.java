package com.tellpal.v2.asset.infrastructure.media;

import java.util.List;

import com.tellpal.v2.asset.api.AssetProcessingRecord;

interface ZipPackagingAdapter {

    List<GeneratedAssetPlan> generatePackages(AssetProcessingRecord assetProcessingRecord);
}
