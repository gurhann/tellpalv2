package com.tellpal.v2.asset.domain;

import java.util.List;
import java.util.Optional;

public interface MediaAssetRepository {

    Optional<MediaAsset> findById(Long id);

    List<MediaAsset> findRecent(int limit);

    Optional<MediaAsset> findByProviderAndObjectPath(StorageProvider provider, String objectPath);

    boolean existsByProviderAndObjectPath(StorageProvider provider, String objectPath);

    MediaAsset save(MediaAsset mediaAsset);
}
