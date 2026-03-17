package com.tellpal.v2.asset.application;

import com.tellpal.v2.asset.api.AssetKind;
import com.tellpal.v2.asset.api.AssetMediaType;
import com.tellpal.v2.asset.api.AssetRecord;
import com.tellpal.v2.asset.api.AssetStorageLocation;
import com.tellpal.v2.asset.api.AssetStorageProvider;
import com.tellpal.v2.asset.domain.MediaAsset;
import com.tellpal.v2.asset.domain.MediaAssetType;
import com.tellpal.v2.asset.domain.MediaKind;
import com.tellpal.v2.asset.domain.StorageProvider;

final class AssetApiMapper {

    private AssetApiMapper() {
    }

    static StorageProvider toDomain(AssetStorageProvider provider) {
        return StorageProvider.valueOf(provider.name());
    }

    static MediaKind toDomain(AssetKind kind) {
        return MediaKind.valueOf(kind.name());
    }

    static AssetRecord toRecord(MediaAsset mediaAsset) {
        return new AssetRecord(
                requireAssetId(mediaAsset),
                new AssetStorageLocation(toApi(mediaAsset.getProvider()), mediaAsset.getObjectPath()),
                toApi(mediaAsset.getMediaType()),
                toApi(mediaAsset.getKind()),
                mediaAsset.getMimeType(),
                mediaAsset.getByteSize(),
                mediaAsset.getChecksumSha256(),
                mediaAsset.getCachedDownloadUrl(),
                mediaAsset.getDownloadUrlCachedAt(),
                mediaAsset.getDownloadUrlExpiresAt(),
                mediaAsset.getCreatedAt(),
                mediaAsset.getUpdatedAt());
    }

    private static AssetStorageProvider toApi(StorageProvider provider) {
        return AssetStorageProvider.valueOf(provider.name());
    }

    private static AssetMediaType toApi(MediaAssetType mediaAssetType) {
        return AssetMediaType.valueOf(mediaAssetType.name());
    }

    private static AssetKind toApi(MediaKind kind) {
        return AssetKind.valueOf(kind.name());
    }

    private static Long requireAssetId(MediaAsset mediaAsset) {
        Long assetId = mediaAsset.getId();
        if (assetId == null || assetId <= 0) {
            throw new IllegalStateException("Media asset must be persisted before mapping to API");
        }
        return assetId;
    }
}
