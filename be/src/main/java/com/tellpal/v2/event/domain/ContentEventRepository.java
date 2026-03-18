package com.tellpal.v2.event.domain;

import java.util.Optional;
import java.util.UUID;

/**
 * Persists content-scoped events that can later participate in attribution flows.
 */
public interface ContentEventRepository {

    /**
     * Returns the event for the immutable event id when it exists.
     */
    Optional<ContentEvent> findById(UUID eventId);

    /**
     * Resolves migrated legacy content events by the profile and historical client-side key pair.
     */
    Optional<ContentEvent> findByProfileIdAndLegacyEventKey(Long profileId, String legacyEventKey);

    /**
     * Checks whether the immutable event id has already been recorded.
     */
    boolean existsById(UUID eventId);

    /**
     * Persists a tracked or migrated content event.
     */
    ContentEvent save(ContentEvent contentEvent);
}
