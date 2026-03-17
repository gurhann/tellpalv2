package com.tellpal.v2.asset.infrastructure.media;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.tellpal.v2.asset.api.AssetKind;
import com.tellpal.v2.asset.api.AssetProcessingContentType;
import com.tellpal.v2.asset.api.AssetProcessingRecord;
import com.tellpal.v2.asset.infrastructure.storage.AssetProcessingPathBuilder;

@Component
class DefaultAudioOptimizationAdapter implements AudioOptimizationAdapter {

    private final AssetProcessingPathBuilder pathBuilder;

    DefaultAudioOptimizationAdapter(AssetProcessingPathBuilder pathBuilder) {
        this.pathBuilder = pathBuilder;
    }

    @Override
    public Optional<GeneratedAssetPlan> generateOptimizedAudio(AssetProcessingRecord assetProcessingRecord) {
        if (assetProcessingRecord.contentType() == AssetProcessingContentType.STORY) {
            return Optional.empty();
        }
        return Optional.of(new GeneratedAssetPlan(
                AssetKind.OPTIMIZED_AUDIO,
                pathBuilder.optimizedAudioPath(assetProcessingRecord),
                "audio/mp4"));
    }
}
