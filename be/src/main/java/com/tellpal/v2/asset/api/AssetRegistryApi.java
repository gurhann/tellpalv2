package com.tellpal.v2.asset.api;

import java.util.List;
import java.util.Optional;

public interface AssetRegistryApi {

    AssetRecord register(RegisterMediaAssetCommand command);

    List<AssetRecord> listRecent(int limit);

    Optional<AssetRecord> findById(Long assetId);

    Optional<AssetRecord> findByStorageLocation(AssetStorageLocation storageLocation);

    AssetRecord updateMetadata(UpdateMediaAssetMetadataCommand command);

    AssetRecord refreshDownloadUrlCache(RefreshMediaAssetDownloadUrlCommand command);
}
