package com.tellpal.v2.event.api;

import java.time.Instant;
import java.util.List;

/**
 * Read-only API for retrieving candidate app events inside an attribution window.
 */
public interface EventAttributionApi {

    /**
     * Returns app events that can be considered for attribution for the given profile and window.
     */
    List<AppEventAttributionCandidate> findAttributionCandidates(
            Long profileId,
            Instant occurredAfterInclusive,
            Instant occurredBeforeInclusive);
}
