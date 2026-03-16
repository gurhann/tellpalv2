package com.tellpal.v2.event.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppEventRepository extends JpaRepository<AppEvent, UUID> {

    Optional<AppEvent> findByProfileIdAndLegacyEventKey(Long profileId, String legacyEventKey);

    List<AppEvent> findByProfileIdOrderByOccurredAtDesc(Long profileId);
}
