package com.tellpal.v2.event.api;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.tellpal.v2.event.domain.AppEventType;
import com.tellpal.v2.event.domain.ContentEventType;
import com.tellpal.v2.shared.domain.LanguageCode;

/**
 * Command types used by the event tracking API.
 */
public final class EventTrackingCommands {

    private EventTrackingCommands() {
    }

    /**
     * Command for recording one content event.
     */
    public record RecordContentEventCommand(
            UUID eventId,
            Long profileId,
            Long contentId,
            LanguageCode languageCode,
            ContentEventType eventType,
            Instant occurredAt,
            UUID sessionId,
            Integer leftPage,
            Integer engagementSeconds,
            Map<String, Object> metadata,
            String legacyEventKey) {

        public RecordContentEventCommand {
            eventId = requireEventId(eventId);
            profileId = requirePositiveId(profileId, "Profile ID must be positive");
            contentId = requirePositiveId(contentId, "Content ID must be positive");
            if (languageCode == null) {
                throw new IllegalArgumentException("Language code must not be null");
            }
            if (eventType == null) {
                throw new IllegalArgumentException("Content event type must not be null");
            }
            if (occurredAt == null) {
                throw new IllegalArgumentException("Occurred at must not be null");
            }
            if (leftPage != null && leftPage <= 0) {
                throw new IllegalArgumentException("Left page must be positive");
            }
            if (engagementSeconds != null && engagementSeconds < 0) {
                throw new IllegalArgumentException("Engagement seconds must not be negative");
            }
            metadata = copyMap(metadata);
            legacyEventKey = normalizeOptionalText(legacyEventKey);
        }
    }

    /**
     * Command for recording one application event.
     */
    public record RecordAppEventCommand(
            UUID eventId,
            Long profileId,
            AppEventType eventType,
            Long contentId,
            Instant occurredAt,
            Map<String, Object> payload,
            String legacyEventKey) {

        public RecordAppEventCommand {
            eventId = requireEventId(eventId);
            profileId = requirePositiveId(profileId, "Profile ID must be positive");
            if (eventType == null) {
                throw new IllegalArgumentException("App event type must not be null");
            }
            if (contentId != null && contentId <= 0) {
                throw new IllegalArgumentException("Content ID must be positive");
            }
            if (occurredAt == null) {
                throw new IllegalArgumentException("Occurred at must not be null");
            }
            payload = copyMap(payload);
            legacyEventKey = normalizeOptionalText(legacyEventKey);
        }
    }

    /**
     * Command for recording a mixed batch of content and application events.
     */
    public record RecordBatchEventsCommand(
            List<RecordContentEventCommand> contentEvents,
            List<RecordAppEventCommand> appEvents) {

        public RecordBatchEventsCommand {
            contentEvents = copyList(contentEvents);
            appEvents = copyList(appEvents);
        }
    }

    private static UUID requireEventId(UUID eventId) {
        if (eventId == null) {
            throw new IllegalArgumentException("Event ID must not be null");
        }
        return eventId;
    }

    private static Long requirePositiveId(Long value, String message) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static <T> List<T> copyList(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private static Map<String, Object> copyMap(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, Object> copy = new LinkedHashMap<>();
        values.forEach((key, value) -> copy.put(requireText(key, "JSON keys must not be blank"), value));
        return Collections.unmodifiableMap(copy);
    }

    private static String requireText(String value, String message) {
        String normalized = normalizeOptionalText(value);
        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private static String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
