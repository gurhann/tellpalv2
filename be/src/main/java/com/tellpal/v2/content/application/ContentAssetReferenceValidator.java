package com.tellpal.v2.content.application;

import org.springframework.stereotype.Component;

import com.tellpal.v2.asset.api.AssetMediaType;
import com.tellpal.v2.asset.api.AssetRecord;
import com.tellpal.v2.asset.api.AssetRegistryApi;
import com.tellpal.v2.content.application.ContentApplicationExceptions.AssetMediaTypeMismatchException;
import com.tellpal.v2.content.application.ContentApplicationExceptions.AssetReferenceNotFoundException;

@Component
final class ContentAssetReferenceValidator {

    private final AssetRegistryApi assetRegistryApi;

    ContentAssetReferenceValidator(AssetRegistryApi assetRegistryApi) {
        this.assetRegistryApi = assetRegistryApi;
    }

    void requireImageAsset(Long assetId, String fieldName) {
        requireAssetType(assetId, fieldName, AssetMediaType.IMAGE);
    }

    void requireAudioAsset(Long assetId, String fieldName) {
        requireAssetType(assetId, fieldName, AssetMediaType.AUDIO);
    }

    private void requireAssetType(Long assetId, String fieldName, AssetMediaType expectedMediaType) {
        if (assetId == null) {
            return;
        }
        AssetRecord asset = assetRegistryApi.findById(assetId)
                .orElseThrow(() -> new AssetReferenceNotFoundException(fieldName, assetId));
        if (asset.mediaType() != expectedMediaType) {
            throw new AssetMediaTypeMismatchException(fieldName, assetId, expectedMediaType, asset.mediaType());
        }
    }
}
