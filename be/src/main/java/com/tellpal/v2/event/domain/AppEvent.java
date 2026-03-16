package com.tellpal.v2.event.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "v2_app_events")
public class AppEvent {

    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "profile_id", nullable = false)
    private Long profileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 64)
    private AppEventType eventType;

    @Column(name = "content_id")
    private Long contentId;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    @Column(name = "ingested_at", nullable = false)
    private OffsetDateTime ingestedAt;

    @Column(name = "payload", columnDefinition = "jsonb")
    private String payload;

    @Column(name = "legacy_event_key", length = 255)
    private String legacyEventKey;

    protected AppEvent() {
    }

    public AppEvent(UUID eventId, Long profileId, AppEventType eventType,
                    OffsetDateTime occurredAt, OffsetDateTime ingestedAt) {
        this.eventId = eventId;
        this.profileId = profileId;
        this.eventType = eventType;
        this.occurredAt = occurredAt;
        this.ingestedAt = ingestedAt;
    }

    public UUID getEventId() { return eventId; }

    public Long getProfileId() { return profileId; }
    public void setProfileId(Long profileId) { this.profileId = profileId; }

    public AppEventType getEventType() { return eventType; }
    public void setEventType(AppEventType eventType) { this.eventType = eventType; }

    public Long getContentId() { return contentId; }
    public void setContentId(Long contentId) { this.contentId = contentId; }

    public OffsetDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(OffsetDateTime occurredAt) { this.occurredAt = occurredAt; }

    public OffsetDateTime getIngestedAt() { return ingestedAt; }
    public void setIngestedAt(OffsetDateTime ingestedAt) { this.ingestedAt = ingestedAt; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public String getLegacyEventKey() { return legacyEventKey; }
    public void setLegacyEventKey(String legacyEventKey) { this.legacyEventKey = legacyEventKey; }
}
