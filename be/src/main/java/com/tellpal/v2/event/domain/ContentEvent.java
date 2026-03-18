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

import com.tellpal.v2.shared.domain.LanguageCode;

/**
 * Immutable content event record.
 *
 * <p>Legacy keys act as alternate idempotency inputs, and page-specific fields are only valid for
 * compatible event types.
 */
@Entity
@Table(name = "content_events")
@Immutable
public class ContentEvent {

    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "profile_id", nullable = false, updatable = false)
    private Long profileId;

    @Column(name = "content_id", nullable = false, updatable = false)
    private Long contentId;

    @Column(name = "language_code", nullable = false, updatable = false, length = 8)
    private LanguageCode languageCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, updatable = false, length = 20)
    private ContentEventType eventType;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "ingested_at", nullable = false, updatable = false)
    private Instant ingestedAt;

    @Column(name = "session_id", updatable = false)
    private UUID sessionId;

    @Column(name = "left_page", updatable = false)
    private Integer leftPage;

    @Column(name = "engagement_seconds", updatable = false)
    private Integer engagementSeconds;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb", updatable = false)
    private Map<String, Object> metadata = new LinkedHashMap<>();

    @Column(name = "legacy_event_key", length = 191, updatable = false)
    private String legacyEventKey;

    protected ContentEvent() {
    }

    private ContentEvent(
            UUID eventId,
            Long profileId,
            Long contentId,
            LanguageCode languageCode,
            ContentEventType eventType,
            Instant occurredAt,
            Instant ingestedAt,
            UUID sessionId,
            Integer leftPage,
            Integer engagementSeconds,
            Map<String, Object> metadata,
            String legacyEventKey) {
        this.eventId = requireEventId(eventId);
        this.profileId = requirePositiveId(profileId, "Profile ID must be positive");
        this.contentId = requirePositiveId(contentId, "Content ID must be positive");
        this.languageCode = requireLanguageCode(languageCode);
        this.eventType = requireEventType(eventType);
        this.occurredAt = requireInstant(occurredAt, "Occurred at must not be null");
        this.ingestedAt = requireInstant(ingestedAt, "Ingested at must not be null");
        this.sessionId = sessionId;
        this.leftPage = normalizeLeftPage(leftPage, eventType);
        this.engagementSeconds = normalizeNonNegative(engagementSeconds, "Engagement seconds must not be negative");
        this.metadata = copyJsonMap(metadata);
        this.legacyEventKey = normalizeOptionalText(legacyEventKey);
    }

    /**
     * Records a new immutable content event.
     */
    public static ContentEvent record(
            UUID eventId,
            Long profileId,
            Long contentId,
            LanguageCode languageCode,
            ContentEventType eventType,
            Instant occurredAt,
            Instant ingestedAt,
            UUID sessionId,
            Integer leftPage,
            Integer engagementSeconds,
            Map<String, Object> metadata,
            String legacyEventKey) {
        return new ContentEvent(
                eventId,
                profileId,
                contentId,
                languageCode,
                eventType,
                occurredAt,
                ingestedAt,
                sessionId,
                leftPage,
                engagementSeconds,
                metadata,
                legacyEventKey);
    }

    public UUID getEventId() {
        return eventId;
    }

    public Long getProfileId() {
        return profileId;
    }

    public Long getContentId() {
        return contentId;
    }

    public LanguageCode getLanguageCode() {
        return languageCode;
    }

    public ContentEventType getEventType() {
        return eventType;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getIngestedAt() {
        return ingestedAt;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public Integer getLeftPage() {
        return leftPage;
    }

    public Integer getEngagementSeconds() {
        return engagementSeconds;
    }

    public Map<String, Object> getMetadata() {
        return Collections.unmodifiableMap(metadata);
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

    private static LanguageCode requireLanguageCode(LanguageCode languageCode) {
        if (languageCode == null) {
            throw new IllegalArgumentException("Language code must not be null");
        }
        return languageCode;
    }

    private static ContentEventType requireEventType(ContentEventType eventType) {
        if (eventType == null) {
            throw new IllegalArgumentException("Content event type must not be null");
        }
        return eventType;
    }

    private static Instant requireInstant(Instant value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static Integer normalizeLeftPage(Integer leftPage, ContentEventType eventType) {
        if (leftPage == null) {
            return null;
        }
        if (leftPage <= 0) {
            throw new IllegalArgumentException("Left page must be positive");
        }
        if (eventType != ContentEventType.EXIT) {
            throw new IllegalArgumentException("Left page can only be recorded for EXIT events");
        }
        return leftPage;
    }

    private static Integer normalizeNonNegative(Integer value, String message) {
        if (value == null) {
            return null;
        }
        if (value < 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
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
