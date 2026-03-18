package com.tellpal.v2.asset.api;

import java.util.List;
import java.util.Optional;

/**
 * Module-facing use cases for media asset registration and lookup.
 */
public interface AssetRegistryApi {

    /**
     * Registers a new media asset for a storage location that is not already known to the system.
     */
    AssetRecord register(RegisterMediaAssetCommand command);

    /**
     * Lists recent assets, capped by the implementation for operational safety.
     */
    List<AssetRecord> listRecent(int limit);

    /**
     * Finds a registered asset by its persistent identifier.
     */
    Optional<AssetRecord> findById(Long assetId);

    /**
     * Finds a registered asset by provider and object path.
     */
    Optional<AssetRecord> findByStorageLocation(AssetStorageLocation storageLocation);

    /**
     * Updates mutable metadata without changing the underlying storage location or asset kind.
     */
    AssetRecord updateMetadata(UpdateMediaAssetMetadataCommand command);

    /**
     * Refreshes the cached signed download URL for an existing asset.
     */
    AssetRecord refreshDownloadUrlCache(RefreshMediaAssetDownloadUrlCommand command);
}
