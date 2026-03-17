package com.tellpal.v2.asset.domain;

import java.util.Optional;

public interface MediaAssetRepository {

    Optional<MediaAsset> findById(Long id);

    Optional<MediaAsset> findByProviderAndObjectPath(StorageProvider provider, String objectPath);

    boolean existsByProviderAndObjectPath(StorageProvider provider, String objectPath);

    MediaAsset save(MediaAsset mediaAsset);
}
