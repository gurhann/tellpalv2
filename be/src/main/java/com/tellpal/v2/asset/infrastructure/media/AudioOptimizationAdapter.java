package com.tellpal.v2.asset.infrastructure.media;

import java.util.Optional;

import com.tellpal.v2.asset.api.AssetProcessingRecord;

interface AudioOptimizationAdapter {

    Optional<GeneratedAssetPlan> generateOptimizedAudio(AssetProcessingRecord assetProcessingRecord);
}
