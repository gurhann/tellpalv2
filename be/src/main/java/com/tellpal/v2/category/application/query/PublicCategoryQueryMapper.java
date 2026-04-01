package com.tellpal.v2.category.application.query;

import com.tellpal.v2.asset.api.AssetRecord;
import com.tellpal.v2.asset.api.ResolvedAssetReference;
import com.tellpal.v2.category.api.PublicCategoryView;
import com.tellpal.v2.category.domain.Category;
import com.tellpal.v2.category.domain.CategoryLocalization;

final class PublicCategoryQueryMapper {

    private PublicCategoryQueryMapper() {
    }

    static PublicCategoryView toView(Category category, CategoryLocalization localization, AssetRecord imageAsset) {
        Long categoryId = requireCategoryId(category);
        return new PublicCategoryView(
                categoryId,
                category.getType().toContentApiType(),
                category.getSlug(),
                localization.getLanguageCode(),
                localization.getName(),
                localization.getDescription(),
                category.isPremium(),
                toResolvedAssetReference(imageAsset));
    }

    private static ResolvedAssetReference toResolvedAssetReference(AssetRecord assetRecord) {
        if (assetRecord == null) {
            return null;
        }
        return new ResolvedAssetReference(
                assetRecord.assetId(),
                assetRecord.kind(),
                assetRecord.storageLocation().provider(),
                assetRecord.storageLocation().objectPath(),
                assetRecord.mimeType(),
                assetRecord.cachedDownloadUrl(),
                assetRecord.downloadUrlExpiresAt());
    }

    private static Long requireCategoryId(Category category) {
        Long categoryId = category.getId();
        if (categoryId == null || categoryId <= 0) {
            throw new IllegalStateException("Category must be persisted before public query mapping");
        }
        return categoryId;
    }
}
