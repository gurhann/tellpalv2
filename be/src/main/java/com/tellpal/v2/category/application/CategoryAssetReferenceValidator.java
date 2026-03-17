package com.tellpal.v2.category.application;

import org.springframework.stereotype.Component;

import com.tellpal.v2.asset.api.AssetMediaType;
import com.tellpal.v2.asset.api.AssetRecord;
import com.tellpal.v2.asset.api.AssetRegistryApi;
import com.tellpal.v2.category.application.CategoryApplicationExceptions.AssetMediaTypeMismatchException;
import com.tellpal.v2.category.application.CategoryApplicationExceptions.AssetReferenceNotFoundException;

@Component
final class CategoryAssetReferenceValidator {

    private final AssetRegistryApi assetRegistryApi;

    CategoryAssetReferenceValidator(AssetRegistryApi assetRegistryApi) {
        this.assetRegistryApi = assetRegistryApi;
    }

    void requireImageAsset(Long assetId, String fieldName) {
        if (assetId == null) {
            return;
        }
        AssetRecord asset = assetRegistryApi.findById(assetId)
                .orElseThrow(() -> new AssetReferenceNotFoundException(fieldName, assetId));
        if (asset.mediaType() != AssetMediaType.IMAGE) {
            throw new AssetMediaTypeMismatchException(fieldName, assetId, AssetMediaType.IMAGE, asset.mediaType());
        }
    }
}
