package com.tellpal.v2.event.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tellpal.v2.event.domain.AppEvent;

interface SpringDataAppEventRepository extends JpaRepository<AppEvent, UUID> {

    Optional<AppEvent> findByProfileIdAndLegacyEventKey(Long profileId, String legacyEventKey);

    List<AppEvent> findByProfileIdAndEventTypeInAndOccurredAtBetweenOrderByOccurredAtDesc(
            Long profileId,
            List<com.tellpal.v2.event.domain.AppEventType> eventTypes,
            Instant occurredAfterInclusive,
            Instant occurredBeforeInclusive);
}
