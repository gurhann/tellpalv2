package com.tellpal.v2.event.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persists user-level application events used for attribution and migration flows.
 */
public interface AppEventRepository {

    /**
     * Returns the event for the immutable event id when it exists.
     */
    Optional<AppEvent> findById(UUID eventId);

    /**
     * Resolves legacy imported events by the profile and historical client-side key pair.
     */
    Optional<AppEvent> findByProfileIdAndLegacyEventKey(Long profileId, String legacyEventKey);

    /**
     * Returns attribution candidates that fall within the requested look-back window.
     */
    List<AppEvent> findAttributionCandidates(Long profileId, Instant occurredAfterInclusive, Instant occurredBeforeInclusive);

    /**
     * Checks whether an immutable event id has already been recorded.
     */
    boolean existsById(UUID eventId);

    /**
     * Persists a tracked or migrated application event.
     */
    AppEvent save(AppEvent appEvent);
}
