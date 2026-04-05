package com.tellpal.v2.asset.infrastructure.media;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.tellpal.v2.asset.api.AssetMediaType;
import com.tellpal.v2.asset.api.AssetProcessingApi;
import com.tellpal.v2.asset.api.AssetProcessingCommands.CompleteAssetProcessingCommand;
import com.tellpal.v2.asset.api.AssetProcessingCommands.FailAssetProcessingCommand;
import com.tellpal.v2.asset.api.AssetProcessingContentType;
import com.tellpal.v2.asset.api.AssetProcessingRecord;
import com.tellpal.v2.asset.api.AssetRecord;
import com.tellpal.v2.asset.api.AssetRegistryApi;
import com.tellpal.v2.asset.api.AssetStorageLocation;
import com.tellpal.v2.asset.api.AssetStorageProvider;
import com.tellpal.v2.asset.api.RegisterMediaAssetCommand;
import com.tellpal.v2.asset.infrastructure.processing.AssetProcessingJobExecutor;

@Component
class RegisteringAssetProcessingJobExecutor implements AssetProcessingJobExecutor {

    private final AssetRegistryApi assetRegistryApi;
    private final AssetProcessingApi assetProcessingApi;
    private final ImageOptimizationAdapter imageOptimizationAdapter;
    private final AudioOptimizationAdapter audioOptimizationAdapter;
    private final ZipPackagingAdapter zipPackagingAdapter;

    RegisteringAssetProcessingJobExecutor(
            AssetRegistryApi assetRegistryApi,
            AssetProcessingApi assetProcessingApi,
            ImageOptimizationAdapter imageOptimizationAdapter,
            AudioOptimizationAdapter audioOptimizationAdapter,
            ZipPackagingAdapter zipPackagingAdapter) {
        this.assetRegistryApi = assetRegistryApi;
        this.assetProcessingApi = assetProcessingApi;
        this.imageOptimizationAdapter = imageOptimizationAdapter;
        this.audioOptimizationAdapter = audioOptimizationAdapter;
        this.zipPackagingAdapter = zipPackagingAdapter;
    }

    @Override
    public void process(AssetProcessingRecord assetProcessingRecord) {
        try {
            AssetStorageProvider targetProvider = resolveTargetProvider(assetProcessingRecord);
            validateSourceAssets(assetProcessingRecord);

            List<GeneratedAssetPlan> plans = new ArrayList<>(imageOptimizationAdapter.generateCoverVariants(assetProcessingRecord));
            audioOptimizationAdapter.generateOptimizedAudio(assetProcessingRecord).ifPresent(plans::add);
            plans.addAll(zipPackagingAdapter.generatePackages(assetProcessingRecord));

            for (GeneratedAssetPlan plan : plans) {
                registerOrReuse(targetProvider, plan);
            }

            assetProcessingApi.complete(new CompleteAssetProcessingCommand(
                    assetProcessingRecord.contentId(),
                    assetProcessingRecord.languageCode()));
        } catch (RuntimeException exception) {
            assetProcessingApi.fail(new FailAssetProcessingCommand(
                    assetProcessingRecord.contentId(),
                    assetProcessingRecord.languageCode(),
                    "PROCESSING_FAILED",
                    exception.getMessage()));
        }
    }

    private void validateSourceAssets(AssetProcessingRecord assetProcessingRecord) {
        requireSourceAsset(assetProcessingRecord.coverSourceAssetId(), AssetMediaType.IMAGE, "coverSourceAssetId");
        if (assetProcessingRecord.contentType() != AssetProcessingContentType.STORY) {
            requireSourceAsset(assetProcessingRecord.audioSourceAssetId(), AssetMediaType.AUDIO, "audioSourceAssetId");
        }
    }

    private AssetRecord requireSourceAsset(Long assetId, AssetMediaType expectedMediaType, String fieldName) {
        if (assetId == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        AssetRecord assetRecord = assetRegistryApi.findById(assetId)
                .orElseThrow(() -> new IllegalArgumentException("Source asset not found: " + assetId));
        if (assetRecord.mediaType() != expectedMediaType) {
            throw new IllegalArgumentException("Source asset " + assetId + " must be " + expectedMediaType);
        }
        return assetRecord;
    }

    private AssetStorageProvider resolveTargetProvider(AssetProcessingRecord assetProcessingRecord) {
        return findSourceAsset(assetProcessingRecord.coverSourceAssetId())
                .or(() -> findSourceAsset(assetProcessingRecord.audioSourceAssetId()))
                .map(record -> record.storageLocation().provider())
                .orElse(AssetStorageProvider.FIREBASE_STORAGE);
    }

    private Optional<AssetRecord> findSourceAsset(Long assetId) {
        if (assetId == null) {
            return Optional.empty();
        }
        return assetRegistryApi.findById(assetId);
    }

    private AssetRecord registerOrReuse(AssetStorageProvider provider, GeneratedAssetPlan plan) {
        AssetStorageLocation storageLocation = new AssetStorageLocation(provider, plan.objectPath());
        return assetRegistryApi.findByStorageLocation(storageLocation)
                .orElseGet(() -> assetRegistryApi.register(new RegisterMediaAssetCommand(
                        provider,
                        plan.objectPath(),
                        plan.kind(),
                        plan.mimeType(),
                        null,
                        null)));
    }
}
