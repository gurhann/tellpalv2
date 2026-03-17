package com.tellpal.v2.event.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.tellpal.v2.event.domain.ContentEvent;
import com.tellpal.v2.event.domain.ContentEventRepository;

@Repository
public class JpaContentEventRepositoryAdapter implements ContentEventRepository {

    private final SpringDataContentEventRepository repository;

    public JpaContentEventRepositoryAdapter(SpringDataContentEventRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<ContentEvent> findById(UUID eventId) {
        return repository.findById(eventId);
    }

    @Override
    public Optional<ContentEvent> findByProfileIdAndLegacyEventKey(Long profileId, String legacyEventKey) {
        return repository.findByProfileIdAndLegacyEventKey(profileId, legacyEventKey);
    }

    @Override
    public boolean existsById(UUID eventId) {
        return repository.existsById(eventId);
    }

    @Override
    public ContentEvent save(ContentEvent contentEvent) {
        return repository.save(contentEvent);
    }
}
