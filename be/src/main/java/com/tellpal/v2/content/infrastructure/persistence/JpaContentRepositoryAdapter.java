package com.tellpal.v2.content.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.tellpal.v2.content.domain.Content;
import com.tellpal.v2.content.domain.ContentRepository;

@Repository
public class JpaContentRepositoryAdapter implements ContentRepository {

    private final SpringDataContentRepository repository;

    public JpaContentRepositoryAdapter(SpringDataContentRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Content> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public Optional<Content> findByIdForAdminRead(Long id) {
        return repository.findByIdForAdminRead(id);
    }

    @Override
    public Optional<Content> findByIdForStoryPageAdminRead(Long id) {
        return repository.findByIdForStoryPageAdminRead(id);
    }

    @Override
    public Optional<Content> findByIdForContributorAdminRead(Long id) {
        return repository.findByIdForContributorAdminRead(id);
    }

    @Override
    public Optional<Content> findByExternalKey(String externalKey) {
        return repository.findByExternalKey(externalKey);
    }

    @Override
    public boolean existsByExternalKey(String externalKey) {
        return repository.existsByExternalKey(externalKey);
    }

    @Override
    public List<Content> findAllActive() {
        return repository.findAllByActiveTrue();
    }

    @Override
    public List<Content> findAllForAdminRead() {
        return repository.findAllForAdminRead();
    }

    @Override
    public List<Content> findAllActiveByIdIn(Collection<Long> contentIds) {
        return repository.findAllByActiveTrueAndIdIn(contentIds);
    }

    @Override
    public boolean existsContributorAssignment(Long contributorId) {
        return repository.existsContributorAssignment(contributorId);
    }

    @Override
    public Content save(Content content) {
        return repository.save(content);
    }

    @Override
    public Content saveAndFlush(Content content) {
        return repository.saveAndFlush(content);
    }
}
