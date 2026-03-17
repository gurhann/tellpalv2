package com.tellpal.v2.asset.infrastructure.media;

import java.util.List;

import com.tellpal.v2.asset.api.AssetProcessingRecord;

interface ImageOptimizationAdapter {

    List<GeneratedAssetPlan> generateCoverVariants(AssetProcessingRecord assetProcessingRecord);
}
