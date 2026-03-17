package com.tellpal.v2.event.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.tellpal.v2.event.domain.AppEvent;
import com.tellpal.v2.event.domain.AppEventRepository;

@Repository
public class JpaAppEventRepositoryAdapter implements AppEventRepository {

    private final SpringDataAppEventRepository repository;

    public JpaAppEventRepositoryAdapter(SpringDataAppEventRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<AppEvent> findById(UUID eventId) {
        return repository.findById(eventId);
    }

    @Override
    public Optional<AppEvent> findByProfileIdAndLegacyEventKey(Long profileId, String legacyEventKey) {
        return repository.findByProfileIdAndLegacyEventKey(profileId, legacyEventKey);
    }

    @Override
    public boolean existsById(UUID eventId) {
        return repository.existsById(eventId);
    }

    @Override
    public AppEvent save(AppEvent appEvent) {
        return repository.save(appEvent);
    }
}
