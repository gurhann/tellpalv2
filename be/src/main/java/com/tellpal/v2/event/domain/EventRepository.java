package com.tellpal.v2.event.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventRepository extends JpaRepository<ContentEvent, UUID> {

    Optional<ContentEvent> findByProfileIdAndLegacyEventKey(Long profileId, String legacyEventKey);

    List<ContentEvent> findByProfileIdOrderByOccurredAtDesc(Long profileId);
}
