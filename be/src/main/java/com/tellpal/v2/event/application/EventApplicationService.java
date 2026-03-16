package com.tellpal.v2.event.application;

import com.tellpal.v2.event.domain.AppEvent;
import com.tellpal.v2.event.domain.AppEventRepository;
import com.tellpal.v2.event.domain.AppEventType;
import com.tellpal.v2.event.domain.ContentEvent;
import com.tellpal.v2.event.domain.ContentEventType;
import com.tellpal.v2.event.domain.EventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class EventApplicationService {

    private final EventRepository eventRepository;
    private final AppEventRepository appEventRepository;

    public EventApplicationService(EventRepository eventRepository,
                                   AppEventRepository appEventRepository) {
        this.eventRepository = eventRepository;
        this.appEventRepository = appEventRepository;
    }

    // --- Batch request records ---

    public record BatchContentEventRequest(
            UUID eventId,
            Long profileId,
            Long contentId,
            String languageCode,
            ContentEventType eventType,
            OffsetDateTime occurredAt,
            OffsetDateTime ingestedAt,
            UUID sessionId,
            Integer leftPage,
            Integer engagementSeconds,
            String metadata,
            String legacyEventKey
    ) {}

    public record BatchAppEventRequest(
            UUID eventId,
            Long profileId,
            AppEventType eventType,
            OffsetDateTime occurredAt,
            OffsetDateTime ingestedAt,
            Long contentId,
            String payload,
            String legacyEventKey
    ) {}

    // --- Content events ---

    public ContentEvent recordContentEvent(UUID eventId, Long profileId, Long contentId, String languageCode,
                                           ContentEventType eventType, OffsetDateTime occurredAt,
                                           OffsetDateTime ingestedAt, UUID sessionId, Integer leftPage,
                                           Integer engagementSeconds, String metadata, String legacyEventKey) {
        // Idempotent by eventId
        var existing = eventRepository.findById(eventId);
        if (existing.isPresent()) {
            return existing.get();
        }

        // Idempotent by legacyEventKey
        if (legacyEventKey != null) {
            var byLegacy = eventRepository.findByProfileIdAndLegacyEventKey(profileId, legacyEventKey);
            if (byLegacy.isPresent()) {
                return byLegacy.get();
            }
        }

        ContentEvent event = new ContentEvent(eventId, profileId, contentId, languageCode,
                eventType, occurredAt, ingestedAt);
        event.setSessionId(sessionId);
        event.setLeftPage(leftPage);
        event.setEngagementSeconds(engagementSeconds);
        event.setMetadata(metadata);
        event.setLegacyEventKey(legacyEventKey);
        return eventRepository.save(event);
    }

    public List<ContentEvent> recordBatchContentEvents(List<BatchContentEventRequest> events) {
        return events.stream()
                .map(r -> recordContentEvent(r.eventId(), r.profileId(), r.contentId(), r.languageCode(),
                        r.eventType(), r.occurredAt(), r.ingestedAt(), r.sessionId(), r.leftPage(),
                        r.engagementSeconds(), r.metadata(), r.legacyEventKey()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ContentEvent getContentEvent(UUID eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ContentEventNotFoundException(eventId));
    }

    // --- App events ---

    public AppEvent recordAppEvent(UUID eventId, Long profileId, AppEventType eventType,
                                   OffsetDateTime occurredAt, OffsetDateTime ingestedAt,
                                   Long contentId, String payload, String legacyEventKey) {
        // Idempotent by eventId
        var existing = appEventRepository.findById(eventId);
        if (existing.isPresent()) {
            return existing.get();
        }

        // Idempotent by legacyEventKey
        if (legacyEventKey != null) {
            var byLegacy = appEventRepository.findByProfileIdAndLegacyEventKey(profileId, legacyEventKey);
            if (byLegacy.isPresent()) {
                return byLegacy.get();
            }
        }

        AppEvent event = new AppEvent(eventId, profileId, eventType, occurredAt, ingestedAt);
        event.setContentId(contentId);
        event.setPayload(payload);
        event.setLegacyEventKey(legacyEventKey);
        return appEventRepository.save(event);
    }

    public List<AppEvent> recordBatchAppEvents(List<BatchAppEventRequest> events) {
        return events.stream()
                .map(r -> recordAppEvent(r.eventId(), r.profileId(), r.eventType(), r.occurredAt(),
                        r.ingestedAt(), r.contentId(), r.payload(), r.legacyEventKey()))
                .toList();
    }

    @Transactional(readOnly = true)
    public AppEvent getAppEvent(UUID eventId) {
        return appEventRepository.findById(eventId)
                .orElseThrow(() -> new AppEventNotFoundException(eventId));
    }
}
