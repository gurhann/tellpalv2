package com.tellpal.v2.asset.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.tellpal.v2.shared.domain.LanguageCode;

public interface AssetProcessingRepository {

    Optional<AssetProcessing> findById(Long id);

    Optional<AssetProcessing> findByContentIdAndLanguageCode(Long contentId, LanguageCode languageCode);

    List<AssetProcessing> findPendingBefore(Instant referenceTime, int limit);

    List<AssetProcessing> findExpiredLeases(Instant referenceTime, int limit);

    List<AssetProcessing> findRecent(int limit);

    AssetProcessing save(AssetProcessing assetProcessing);
}
