package com.tellpal.v2.presentation.api.public_api;

import com.tellpal.v2.event.application.EventApplicationService;
import com.tellpal.v2.event.domain.AppEventType;
import com.tellpal.v2.event.domain.ContentEventType;
import com.tellpal.v2.presentation.dto.event.BatchEventRequest;
import com.tellpal.v2.presentation.dto.event.RecordAppEventRequest;
import com.tellpal.v2.presentation.dto.event.RecordContentEventRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@RestController
public class EventController {

    private final EventApplicationService eventApplicationService;

    public EventController(EventApplicationService eventApplicationService) {
        this.eventApplicationService = eventApplicationService;
    }

    @PostMapping("/api/events/content")
    public ResponseEntity<Void> recordContentEvent(@RequestBody RecordContentEventRequest request) {
        OffsetDateTime ingestedAt = OffsetDateTime.now(ZoneOffset.UTC);
        ContentEventType eventType;
        try {
            eventType = ContentEventType.valueOf(request.eventType());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        eventApplicationService.recordContentEvent(
                request.eventId(),
                request.profileId(),
                request.contentId(),
                request.languageCode(),
                eventType,
                request.occurredAt(),
                ingestedAt,
                request.sessionId(),
                request.leftPage(),
                request.engagementSeconds(),
                request.metadata(),
                request.legacyEventKey()
        );
        return ResponseEntity.status(201).build();
    }

    @PostMapping("/api/events/app")
    public ResponseEntity<Void> recordAppEvent(@RequestBody RecordAppEventRequest request) {
        OffsetDateTime ingestedAt = OffsetDateTime.now(ZoneOffset.UTC);
        AppEventType eventType;
        try {
            eventType = AppEventType.valueOf(request.eventType());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        eventApplicationService.recordAppEvent(
                request.eventId(),
                request.profileId(),
                eventType,
                request.occurredAt(),
                ingestedAt,
                request.contentId(),
                request.payload(),
                request.legacyEventKey()
        );
        return ResponseEntity.status(201).build();
    }

    @PostMapping("/api/events/batch")
    public ResponseEntity<Void> recordBatchEvents(@RequestBody BatchEventRequest request) {
        OffsetDateTime ingestedAt = OffsetDateTime.now(ZoneOffset.UTC);

        List<EventApplicationService.BatchContentEventRequest> contentEvents = request.contentEvents().stream()
                .map(r -> {
                    ContentEventType eventType = ContentEventType.valueOf(r.eventType());
                    return new EventApplicationService.BatchContentEventRequest(
                            r.eventId(), r.profileId(), r.contentId(), r.languageCode(),
                            eventType, r.occurredAt(), ingestedAt,
                            r.sessionId(), r.leftPage(), r.engagementSeconds(),
                            r.metadata(), r.legacyEventKey()
                    );
                })
                .toList();

        List<EventApplicationService.BatchAppEventRequest> appEvents = request.appEvents().stream()
                .map(r -> {
                    AppEventType eventType = AppEventType.valueOf(r.eventType());
                    return new EventApplicationService.BatchAppEventRequest(
                            r.eventId(), r.profileId(), eventType,
                            r.occurredAt(), ingestedAt,
                            r.contentId(), r.payload(), r.legacyEventKey()
                    );
                })
                .toList();

        eventApplicationService.recordBatchContentEvents(contentEvents);
        eventApplicationService.recordBatchAppEvents(appEvents);

        return ResponseEntity.ok().build();
    }
}
