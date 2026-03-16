package com.tellpal.v2.asset.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssetProcessingRepository extends JpaRepository<AssetProcessing, Long> {

    Optional<AssetProcessing> findByContentIdAndLanguageCode(Long contentId, String languageCode);

    List<AssetProcessing> findByContentId(Long contentId);

    List<AssetProcessing> findByStatus(ProcessingStatus status);
}
