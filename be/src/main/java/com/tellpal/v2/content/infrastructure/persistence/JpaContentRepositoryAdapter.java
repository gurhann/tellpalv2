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
    public List<Content> findAllActiveByIdIn(Collection<Long> contentIds) {
        return repository.findAllByActiveTrueAndIdIn(contentIds);
    }

    @Override
    public Content save(Content content) {
        return repository.save(content);
    }
}
