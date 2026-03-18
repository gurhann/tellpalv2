package com.tellpal.v2.event.api;

import java.time.Instant;
import java.util.UUID;

/**
 * Candidate app event that can be used to attribute a purchase or another downstream action.
 */
public record AppEventAttributionCandidate(
        UUID eventId,
        Long profileId,
        AttributionAppEventType eventType,
        Long contentId,
        Instant occurredAt) {

    public AppEventAttributionCandidate {
        if (eventId == null) {
            throw new IllegalArgumentException("Event ID must not be null");
        }
        if (profileId == null || profileId <= 0) {
            throw new IllegalArgumentException("Profile ID must be positive");
        }
        if (eventType == null) {
            throw new IllegalArgumentException("Event type must not be null");
        }
        if (occurredAt == null) {
            throw new IllegalArgumentException("Occurred at must not be null");
        }
        if (contentId != null && contentId <= 0) {
            throw new IllegalArgumentException("Content ID must be positive when present");
        }
    }
}
