package com.tellpal.v2.asset.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.tellpal.v2.asset.domain.AssetProcessing;
import com.tellpal.v2.asset.domain.AssetProcessingStatus;
import com.tellpal.v2.shared.domain.LanguageCode;

interface SpringDataAssetProcessingRepository extends JpaRepository<AssetProcessing, Long> {

    Optional<AssetProcessing> findByContentIdAndLanguageCode(Long contentId, LanguageCode languageCode);

    List<AssetProcessing> findByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAscCreatedAtAsc(
            AssetProcessingStatus status,
            Instant referenceTime,
            Pageable pageable);

    List<AssetProcessing> findByStatusAndLeaseExpiresAtLessThanEqualOrderByLeaseExpiresAtAscCreatedAtAsc(
            AssetProcessingStatus status,
            Instant referenceTime,
            Pageable pageable);

    List<AssetProcessing> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
