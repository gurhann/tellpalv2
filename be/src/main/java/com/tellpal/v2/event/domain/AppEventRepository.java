package com.tellpal.v2.event.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppEventRepository {

    Optional<AppEvent> findById(UUID eventId);

    Optional<AppEvent> findByProfileIdAndLegacyEventKey(Long profileId, String legacyEventKey);

    List<AppEvent> findAttributionCandidates(Long profileId, Instant occurredAfterInclusive, Instant occurredBeforeInclusive);

    boolean existsById(UUID eventId);

    AppEvent save(AppEvent appEvent);
}
