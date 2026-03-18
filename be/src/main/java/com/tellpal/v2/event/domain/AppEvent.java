package com.tellpal.v2.event.domain;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Immutable application event record.
 *
 * <p>Some event types require a content reference, and optional legacy keys act as alternate
 * idempotency inputs during migration.
 */
@Entity
@Table(name = "app_events")
@Immutable
public class AppEvent {

    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "profile_id", nullable = false, updatable = false)
    private Long profileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, updatable = false, length = 32)
    private AppEventType eventType;

    @Column(name = "content_id", updatable = false)
    private Long contentId;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "ingested_at", nullable = false, updatable = false)
    private Instant ingestedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb", updatable = false)
    private Map<String, Object> payload = new LinkedHashMap<>();

    @Column(name = "legacy_event_key", length = 191, updatable = false)
    private String legacyEventKey;

    protected AppEvent() {
    }

    private AppEvent(
            UUID eventId,
            Long profileId,
            AppEventType eventType,
            Long contentId,
            Instant occurredAt,
            Instant ingestedAt,
            Map<String, Object> payload,
            String legacyEventKey) {
        this.eventId = requireEventId(eventId);
        this.profileId = requirePositiveId(profileId, "Profile ID must be positive");
        this.eventType = requireEventType(eventType);
        this.contentId = normalizeContentId(contentId, eventType);
        this.occurredAt = requireInstant(occurredAt, "Occurred at must not be null");
        this.ingestedAt = requireInstant(ingestedAt, "Ingested at must not be null");
        this.payload = copyJsonMap(payload);
        this.legacyEventKey = normalizeOptionalText(legacyEventKey);
    }

    /**
     * Records a new immutable application event.
     */
    public static AppEvent record(
            UUID eventId,
            Long profileId,
            AppEventType eventType,
            Long contentId,
            Instant occurredAt,
            Instant ingestedAt,
            Map<String, Object> payload,
            String legacyEventKey) {
        return new AppEvent(
                eventId,
                profileId,
                eventType,
                contentId,
                occurredAt,
                ingestedAt,
                payload,
                legacyEventKey);
    }

    public UUID getEventId() {
        return eventId;
    }

    public Long getProfileId() {
        return profileId;
    }

    public AppEventType getEventType() {
        return eventType;
    }

    public Long getContentId() {
        return contentId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getIngestedAt() {
        return ingestedAt;
    }

    public Map<String, Object> getPayload() {
        return Collections.unmodifiableMap(payload);
    }

    public String getLegacyEventKey() {
        return legacyEventKey;
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

    private static AppEventType requireEventType(AppEventType eventType) {
        if (eventType == null) {
            throw new IllegalArgumentException("App event type must not be null");
        }
        return eventType;
    }

    private static Instant requireInstant(Instant value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static Long normalizeContentId(Long contentId, AppEventType eventType) {
        if (contentId == null) {
            if (eventType == AppEventType.LOCKED_CONTENT_CLICKED) {
                throw new IllegalArgumentException("Locked content click events must include a content ID");
            }
            return null;
        }
        return requirePositiveId(contentId, "Content ID must be positive");
    }

    private static Map<String, Object> copyJsonMap(Map<String, Object> value) {
        LinkedHashMap<String, Object> copy = new LinkedHashMap<>();
        if (value == null || value.isEmpty()) {
            return copy;
        }
        value.forEach((key, entryValue) -> copy.put(requireText(key, "JSON keys must not be blank"), entryValue));
        return copy;
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
