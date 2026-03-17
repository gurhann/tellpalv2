package com.tellpal.v2.asset.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tellpal.v2.asset.domain.MediaAsset;
import com.tellpal.v2.asset.domain.StorageProvider;

interface SpringDataMediaAssetRepository extends JpaRepository<MediaAsset, Long> {

    Optional<MediaAsset> findByProviderAndObjectPath(StorageProvider provider, String objectPath);

    boolean existsByProviderAndObjectPath(StorageProvider provider, String objectPath);
}
