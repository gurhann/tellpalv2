package com.tellpal.v2.event.web.mobile;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tellpal.v2.event.api.EventTrackingApi;
import com.tellpal.v2.event.api.EventTrackingCommands.RecordAppEventCommand;
import com.tellpal.v2.event.api.EventTrackingCommands.RecordBatchEventsCommand;
import com.tellpal.v2.event.api.EventTrackingCommands.RecordContentEventCommand;
import com.tellpal.v2.event.domain.AppEventType;
import com.tellpal.v2.event.domain.ContentEventType;
import com.tellpal.v2.shared.domain.LanguageCode;
import com.tellpal.v2.user.api.AuthenticatedAppUser;

@RestController
@RequestMapping("/api/events")
public class EventMobileController {

    private final AuthenticatedMobileEventUserResolver authenticatedMobileEventUserResolver;
    private final EventTrackingApi eventTrackingApi;

    public EventMobileController(
            AuthenticatedMobileEventUserResolver authenticatedMobileEventUserResolver,
            EventTrackingApi eventTrackingApi) {
        this.authenticatedMobileEventUserResolver = authenticatedMobileEventUserResolver;
        this.eventTrackingApi = eventTrackingApi;
    }

    @PostMapping("/content")
    public EventReceiptResponse recordContentEvent(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @Valid @RequestBody RecordContentEventRequest request) {
        AuthenticatedAppUser currentUser = authenticatedMobileEventUserResolver.resolveCurrentUser(authorizationHeader);
        return EventReceiptResponse.from(eventTrackingApi.recordContentEvent(request.toCommand(currentUser)));
    }

    @PostMapping("/app")
    public EventReceiptResponse recordAppEvent(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @Valid @RequestBody RecordAppEventRequest request) {
        AuthenticatedAppUser currentUser = authenticatedMobileEventUserResolver.resolveCurrentUser(authorizationHeader);
        return EventReceiptResponse.from(eventTrackingApi.recordAppEvent(request.toCommand(currentUser)));
    }

    @PostMapping("/batch")
    public EventBatchResponse recordBatchEvents(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @Valid @RequestBody RecordBatchEventsRequest request) {
        AuthenticatedAppUser currentUser = authenticatedMobileEventUserResolver.resolveCurrentUser(authorizationHeader);
        return EventBatchResponse.from(eventTrackingApi.recordBatchEvents(request.toCommand(currentUser)));
    }
}

record RecordContentEventRequest(
        @NotNull(message = "eventId is required")
        UUID eventId,
        @Positive(message = "contentId must be positive")
        Long contentId,
        @NotNull(message = "languageCode is required")
        String languageCode,
        @NotNull(message = "eventType is required")
        ContentEventType eventType,
        @NotNull(message = "occurredAt is required")
        Instant occurredAt,
        UUID sessionId,
        @Min(value = 1, message = "leftPage must be positive")
        Integer leftPage,
        @Min(value = 0, message = "engagementSeconds must not be negative")
        Integer engagementSeconds,
        Map<String, Object> metadata,
        String legacyEventKey) {

    RecordContentEventCommand toCommand(AuthenticatedAppUser currentUser) {
        return new RecordContentEventCommand(
                eventId,
                currentUser.primaryProfileId(),
                contentId,
                LanguageCode.from(languageCode),
                eventType,
                occurredAt,
                sessionId,
                leftPage,
                engagementSeconds,
                metadata,
                legacyEventKey);
    }
}

record RecordAppEventRequest(
        @NotNull(message = "eventId is required")
        UUID eventId,
        @NotNull(message = "eventType is required")
        AppEventType eventType,
        @Positive(message = "contentId must be positive")
        Long contentId,
        @NotNull(message = "occurredAt is required")
        Instant occurredAt,
        Map<String, Object> payload,
        String legacyEventKey) {

    RecordAppEventCommand toCommand(AuthenticatedAppUser currentUser) {
        return new RecordAppEventCommand(
                eventId,
                currentUser.primaryProfileId(),
                eventType,
                contentId,
                occurredAt,
                payload,
                legacyEventKey);
    }
}

record RecordBatchEventsRequest(
        @NotNull(message = "contentEvents is required")
        List<@Valid RecordContentEventRequest> contentEvents,
        @NotNull(message = "appEvents is required")
        List<@Valid RecordAppEventRequest> appEvents) {

    RecordBatchEventsCommand toCommand(AuthenticatedAppUser currentUser) {
        return new RecordBatchEventsCommand(
                contentEvents.stream().map(request -> request.toCommand(currentUser)).toList(),
                appEvents.stream().map(request -> request.toCommand(currentUser)).toList());
    }
}
