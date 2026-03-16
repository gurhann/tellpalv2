package com.tellpal.v2.shared.infrastructure.persistence;

import com.tellpal.v2.shared.domain.MediaKind;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MediaAssetRepository extends JpaRepository<MediaAssetEntity, Long> {

    Optional<MediaAssetEntity> findByProviderAndObjectPath(String provider, String objectPath);

    List<MediaAssetEntity> findByKind(MediaKind kind);
}
