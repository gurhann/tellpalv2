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
@Table(name = "v2_content_events")
public class ContentEvent {

    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "profile_id", nullable = false)
    private Long profileId;

    @Column(name = "content_id", nullable = false)
    private Long contentId;

    @Column(name = "language_code", nullable = false, length = 10)
    private String languageCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 32)
    private ContentEventType eventType;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    @Column(name = "ingested_at", nullable = false)
    private OffsetDateTime ingestedAt;

    @Column(name = "session_id")
    private UUID sessionId;

    @Column(name = "left_page")
    private Integer leftPage;

    @Column(name = "engagement_seconds")
    private Integer engagementSeconds;

    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "legacy_event_key", length = 255)
    private String legacyEventKey;

    protected ContentEvent() {
    }

    public ContentEvent(UUID eventId, Long profileId, Long contentId, String languageCode,
                        ContentEventType eventType, OffsetDateTime occurredAt, OffsetDateTime ingestedAt) {
        this.eventId = eventId;
        this.profileId = profileId;
        this.contentId = contentId;
        this.languageCode = languageCode;
        this.eventType = eventType;
        this.occurredAt = occurredAt;
        this.ingestedAt = ingestedAt;
    }

    public UUID getEventId() { return eventId; }

    public Long getProfileId() { return profileId; }
    public void setProfileId(Long profileId) { this.profileId = profileId; }

    public Long getContentId() { return contentId; }
    public void setContentId(Long contentId) { this.contentId = contentId; }

    public String getLanguageCode() { return languageCode; }
    public void setLanguageCode(String languageCode) { this.languageCode = languageCode; }

    public ContentEventType getEventType() { return eventType; }
    public void setEventType(ContentEventType eventType) { this.eventType = eventType; }

    public OffsetDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(OffsetDateTime occurredAt) { this.occurredAt = occurredAt; }

    public OffsetDateTime getIngestedAt() { return ingestedAt; }
    public void setIngestedAt(OffsetDateTime ingestedAt) { this.ingestedAt = ingestedAt; }

    public UUID getSessionId() { return sessionId; }
    public void setSessionId(UUID sessionId) { this.sessionId = sessionId; }

    public Integer getLeftPage() { return leftPage; }
    public void setLeftPage(Integer leftPage) { this.leftPage = leftPage; }

    public Integer getEngagementSeconds() { return engagementSeconds; }
    public void setEngagementSeconds(Integer engagementSeconds) { this.engagementSeconds = engagementSeconds; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public String getLegacyEventKey() { return legacyEventKey; }
    public void setLegacyEventKey(String legacyEventKey) { this.legacyEventKey = legacyEventKey; }
}
