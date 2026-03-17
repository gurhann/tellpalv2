package com.tellpal.v2.asset.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.tellpal.v2.asset.domain.MediaAsset;
import com.tellpal.v2.asset.domain.MediaAssetRepository;
import com.tellpal.v2.asset.domain.StorageProvider;

@Repository
public class JpaMediaAssetRepositoryAdapter implements MediaAssetRepository {

    private final SpringDataMediaAssetRepository repository;

    public JpaMediaAssetRepositoryAdapter(SpringDataMediaAssetRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<MediaAsset> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<MediaAsset> findRecent(int limit) {
        return repository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit));
    }

    @Override
    public Optional<MediaAsset> findByProviderAndObjectPath(StorageProvider provider, String objectPath) {
        return repository.findByProviderAndObjectPath(provider, objectPath);
    }

    @Override
    public boolean existsByProviderAndObjectPath(StorageProvider provider, String objectPath) {
        return repository.existsByProviderAndObjectPath(provider, objectPath);
    }

    @Override
    public MediaAsset save(MediaAsset mediaAsset) {
        return repository.save(mediaAsset);
    }
}
