package com.tellpal.v2.asset.application;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tellpal.v2.asset.api.AssetKind;
import com.tellpal.v2.asset.api.AssetProcessingApi;
import com.tellpal.v2.asset.api.AssetProcessingRecord;
import com.tellpal.v2.asset.api.AssetProcessingState;
import com.tellpal.v2.asset.api.AssetRecord;
import com.tellpal.v2.asset.api.AssetRegistryApi;
import com.tellpal.v2.asset.api.AssetStorageLocation;
import com.tellpal.v2.asset.api.AssetStorageProvider;
import com.tellpal.v2.asset.api.ContentAssetBundleApi;
import com.tellpal.v2.asset.api.ContentCoverVariants;
import com.tellpal.v2.asset.api.ContentDeliveryAssets;
import com.tellpal.v2.asset.api.ContentPackageAssets;
import com.tellpal.v2.asset.api.ResolvedAssetReference;
import com.tellpal.v2.asset.infrastructure.storage.AssetProcessingPathBuilder;
import com.tellpal.v2.shared.domain.LanguageCode;

@Service
@Transactional(readOnly = true)
public class ContentAssetBundleService implements ContentAssetBundleApi {

    private final AssetProcessingApi assetProcessingApi;
    private final AssetRegistryApi assetRegistryApi;
    private final AssetProcessingPathBuilder assetProcessingPathBuilder;

    public ContentAssetBundleService(
            AssetProcessingApi assetProcessingApi,
            AssetRegistryApi assetRegistryApi,
            AssetProcessingPathBuilder assetProcessingPathBuilder) {
        this.assetProcessingApi = assetProcessingApi;
        this.assetRegistryApi = assetRegistryApi;
        this.assetProcessingPathBuilder = assetProcessingPathBuilder;
    }

    @Override
    public Optional<ContentDeliveryAssets> findForLocalization(Long contentId, LanguageCode languageCode) {
        return assetProcessingApi.findByLocalization(requireContentId(contentId), requireLanguageCode(languageCode))
                .filter(record -> record.status() == AssetProcessingState.COMPLETED)
                .map(this::toDeliveryAssets);
    }

    private ContentDeliveryAssets toDeliveryAssets(AssetProcessingRecord assetProcessingRecord) {
        AssetStorageProvider targetProvider = resolveTargetProvider(assetProcessingRecord);
        return new ContentDeliveryAssets(
                new ContentCoverVariants(
                        resolveGeneratedAsset(assetProcessingRecord, targetProvider, AssetKind.THUMBNAIL_PHONE),
                        resolveGeneratedAsset(assetProcessingRecord, targetProvider, AssetKind.THUMBNAIL_TABLET),
                        resolveGeneratedAsset(assetProcessingRecord, targetProvider, AssetKind.DETAIL_PHONE),
                        resolveGeneratedAsset(assetProcessingRecord, targetProvider, AssetKind.DETAIL_TABLET)),
                new ContentPackageAssets(
                        resolveGeneratedAsset(assetProcessingRecord, targetProvider, AssetKind.CONTENT_ZIP),
                        resolveGeneratedAsset(assetProcessingRecord, targetProvider, AssetKind.CONTENT_ZIP_PART1),
                        resolveGeneratedAsset(assetProcessingRecord, targetProvider, AssetKind.CONTENT_ZIP_PART2)),
                resolveGeneratedAsset(assetProcessingRecord, targetProvider, AssetKind.OPTIMIZED_AUDIO));
    }

    private ResolvedAssetReference resolveGeneratedAsset(
            AssetProcessingRecord assetProcessingRecord,
            AssetStorageProvider provider,
            AssetKind assetKind) {
        return assetRegistryApi.findByStorageLocation(new AssetStorageLocation(
                        provider,
                        resolveObjectPath(assetProcessingRecord, assetKind)))
                .map(ContentAssetBundleService::toResolvedAssetReference)
                .orElse(null);
    }

    private String resolveObjectPath(AssetProcessingRecord assetProcessingRecord, AssetKind assetKind) {
        return switch (assetKind) {
            case THUMBNAIL_PHONE, THUMBNAIL_TABLET, DETAIL_PHONE, DETAIL_TABLET ->
                assetProcessingPathBuilder.coverVariantPath(assetProcessingRecord, assetKind);
            case OPTIMIZED_AUDIO -> assetProcessingPathBuilder.optimizedAudioPath(assetProcessingRecord);
            case CONTENT_ZIP, CONTENT_ZIP_PART1, CONTENT_ZIP_PART2 ->
                assetProcessingPathBuilder.packagePath(assetProcessingRecord, assetKind);
            default -> throw new IllegalArgumentException("Unsupported delivery asset kind: " + assetKind);
        };
    }

    private AssetStorageProvider resolveTargetProvider(AssetProcessingRecord assetProcessingRecord) {
        return findSourceAsset(assetProcessingRecord.coverSourceAssetId())
                .or(() -> findSourceAsset(assetProcessingRecord.audioSourceAssetId()))
                .map(assetRecord -> assetRecord.storageLocation().provider())
                .orElse(AssetStorageProvider.LOCAL_STUB);
    }

    private Optional<AssetRecord> findSourceAsset(Long assetId) {
        if (assetId == null) {
            return Optional.empty();
        }
        return assetRegistryApi.findById(assetId);
    }

    private static ResolvedAssetReference toResolvedAssetReference(AssetRecord assetRecord) {
        return new ResolvedAssetReference(
                assetRecord.assetId(),
                assetRecord.kind(),
                assetRecord.storageLocation().provider(),
                assetRecord.storageLocation().objectPath(),
                assetRecord.mimeType(),
                assetRecord.cachedDownloadUrl(),
                assetRecord.downloadUrlExpiresAt());
    }

    private static Long requireContentId(Long contentId) {
        if (contentId == null || contentId <= 0) {
            throw new IllegalArgumentException("Content ID must be positive");
        }
        return contentId;
    }

    private static LanguageCode requireLanguageCode(LanguageCode languageCode) {
        if (languageCode == null) {
            throw new IllegalArgumentException("Language code must not be null");
        }
        return languageCode;
    }
}
