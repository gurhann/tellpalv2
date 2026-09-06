package com.tellpal.v2.asset.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.tellpal.v2.asset.domain.AssetProcessing;
import com.tellpal.v2.asset.domain.AssetProcessingRepository;
import com.tellpal.v2.asset.domain.AssetProcessingStatus;
import com.tellpal.v2.shared.domain.LanguageCode;
import com.tellpal.v2.asset.api.AssetProcessingTarget;
import com.tellpal.v2.asset.api.AssetProcessingTargetScope;

@Repository
public class JpaAssetProcessingRepositoryAdapter implements AssetProcessingRepository {

    private final SpringDataAssetProcessingRepository repository;

    public JpaAssetProcessingRepositoryAdapter(SpringDataAssetProcessingRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<AssetProcessing> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public Optional<AssetProcessing> findByContentIdAndLanguageCode(Long contentId, LanguageCode languageCode) {
        return repository.findByTargetScopeAndContentIdAndLanguageCode(
                AssetProcessingTargetScope.LOCALIZATION, contentId, languageCode);
    }

    @Override
    public Optional<AssetProcessing> findByTarget(AssetProcessingTarget target) {
        if (target.isContent()) {
            return findByContent(target.contentId());
        }
        return findByContentIdAndLanguageCode(target.contentId(), target.languageCode());
    }

    @Override
    public Optional<AssetProcessing> findByContent(Long contentId) {
        return repository.findByTargetScopeAndContentId(AssetProcessingTargetScope.CONTENT, contentId);
    }

    @Override
    public List<AssetProcessing> findPendingBefore(Instant referenceTime, int limit) {
        return repository.findByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAscCreatedAtAsc(
                AssetProcessingStatus.PENDING,
                referenceTime,
                PageRequest.of(0, sanitizeLimit(limit)));
    }

    @Override
    public List<AssetProcessing> findExpiredLeases(Instant referenceTime, int limit) {
        return repository.findByStatusAndLeaseExpiresAtLessThanEqualOrderByLeaseExpiresAtAscCreatedAtAsc(
                AssetProcessingStatus.PROCESSING,
                referenceTime,
                PageRequest.of(0, sanitizeLimit(limit)));
    }

    @Override
    public List<AssetProcessing> findRecent(int limit) {
        return repository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, sanitizeLimit(limit)));
    }

    @Override
    public AssetProcessing save(AssetProcessing assetProcessing) {
        return repository.save(assetProcessing);
    }

    private static int sanitizeLimit(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be positive");
        }
        return Math.min(limit, 100);
    }
}
