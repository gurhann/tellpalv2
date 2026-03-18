package com.tellpal.v2.event.application;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tellpal.v2.content.api.ContentLookupApi;
import com.tellpal.v2.event.api.AppEventAttributionCandidate;
import com.tellpal.v2.event.api.EventAttributionApi;
import com.tellpal.v2.event.api.EventTrackingApi;
import com.tellpal.v2.event.api.EventTrackingCommands.RecordAppEventCommand;
import com.tellpal.v2.event.api.EventTrackingCommands.RecordBatchEventsCommand;
import com.tellpal.v2.event.api.EventTrackingCommands.RecordContentEventCommand;
import com.tellpal.v2.event.api.EventTrackingResults.EventBatchIngestResult;
import com.tellpal.v2.event.api.EventTrackingResults.EventIngestReceipt;
import com.tellpal.v2.event.api.EventTrackingResults.EventIngestStatus;
import com.tellpal.v2.event.api.EventTrackingResults.EventStream;
import com.tellpal.v2.event.application.EventApplicationExceptions.ReferencedContentNotFoundException;
import com.tellpal.v2.event.domain.AppEvent;
import com.tellpal.v2.event.domain.AppEventRepository;
import com.tellpal.v2.event.domain.ContentEvent;
import com.tellpal.v2.event.domain.ContentEventRepository;

/**
 * Application service for event ingest and attribution lookup.
 *
 * <p>The service validates referenced content, deduplicates by event ID and legacy key, and keeps
 * event receipts stable for callers.
 */
@Service
public class EventTrackingService implements EventTrackingApi, EventAttributionApi {

    private final ContentEventRepository contentEventRepository;
    private final AppEventRepository appEventRepository;
    private final ContentLookupApi contentLookupApi;
    private final Clock clock;

    public EventTrackingService(
            ContentEventRepository contentEventRepository,
            AppEventRepository appEventRepository,
            ContentLookupApi contentLookupApi,
            Clock clock) {
        this.contentEventRepository = contentEventRepository;
        this.appEventRepository = appEventRepository;
        this.contentLookupApi = contentLookupApi;
        this.clock = clock;
    }

    @Override
    /**
     * Records one content event after verifying the referenced content exists.
     */
    @Transactional
    public EventIngestReceipt recordContentEvent(RecordContentEventCommand command) {
        requireContentExists(command.contentId());
        return recordContentEventInternal(command);
    }

    @Override
    /**
     * Records one application event, validating optional content references when present.
     */
    @Transactional
    public EventIngestReceipt recordAppEvent(RecordAppEventCommand command) {
        if (command.contentId() != null) {
            requireContentExists(command.contentId());
        }
        return recordAppEventInternal(command);
    }

    @Override
    /**
     * Records a mixed batch of content and application events.
     */
    @Transactional
    public EventBatchIngestResult recordBatchEvents(RecordBatchEventsCommand command) {
        List<EventIngestReceipt> receipts = new ArrayList<>();
        command.contentEvents().forEach(contentCommand -> {
            requireContentExists(contentCommand.contentId());
            receipts.add(recordContentEventInternal(contentCommand));
        });
        command.appEvents().forEach(appCommand -> {
            if (appCommand.contentId() != null) {
                requireContentExists(appCommand.contentId());
            }
            receipts.add(recordAppEventInternal(appCommand));
        });
        return new EventBatchIngestResult(receipts);
    }

    @Override
    /**
     * Returns attribution candidates inside the requested window for a profile.
     */
    @Transactional(readOnly = true)
    public List<AppEventAttributionCandidate> findAttributionCandidates(
            Long profileId,
            Instant occurredAfterInclusive,
            Instant occurredBeforeInclusive) {
        if (profileId == null || profileId <= 0) {
            throw new IllegalArgumentException("Profile ID must be positive");
        }
        if (occurredAfterInclusive == null || occurredBeforeInclusive == null) {
            throw new IllegalArgumentException("Attribution window bounds must not be null");
        }
        if (occurredAfterInclusive.isAfter(occurredBeforeInclusive)) {
            throw new IllegalArgumentException("Attribution window start must not be after its end");
        }
        return appEventRepository.findAttributionCandidates(profileId, occurredAfterInclusive, occurredBeforeInclusive)
                .stream()
                .map(EventApiMapper::toAttributionCandidate)
                .toList();
    }

    private EventIngestReceipt recordContentEventInternal(RecordContentEventCommand command) {
        return contentEventRepository.findById(command.eventId())
                .map(existing -> duplicateReceipt(
                        existing.getEventId(),
                        EventStream.CONTENT,
                        EventIngestStatus.DUPLICATE_EVENT_ID,
                        existing.getIngestedAt()))
                .orElseGet(() -> command.legacyEventKey() == null
                        ? persistContentEvent(command)
                        : contentEventRepository.findByProfileIdAndLegacyEventKey(
                                        command.profileId(),
                                        command.legacyEventKey())
                                .map(existing -> duplicateReceipt(
                                        existing.getEventId(),
                                        EventStream.CONTENT,
                                        EventIngestStatus.DUPLICATE_LEGACY_EVENT_KEY,
                                        existing.getIngestedAt()))
                                .orElseGet(() -> persistContentEvent(command)));
    }

    private EventIngestReceipt recordAppEventInternal(RecordAppEventCommand command) {
        return appEventRepository.findById(command.eventId())
                .map(existing -> duplicateReceipt(
                        existing.getEventId(),
                        EventStream.APP,
                        EventIngestStatus.DUPLICATE_EVENT_ID,
                        existing.getIngestedAt()))
                .orElseGet(() -> command.legacyEventKey() == null
                        ? persistAppEvent(command)
                        : appEventRepository.findByProfileIdAndLegacyEventKey(
                                        command.profileId(),
                                        command.legacyEventKey())
                                .map(existing -> duplicateReceipt(
                                        existing.getEventId(),
                                        EventStream.APP,
                                        EventIngestStatus.DUPLICATE_LEGACY_EVENT_KEY,
                                        existing.getIngestedAt()))
                                .orElseGet(() -> persistAppEvent(command)));
    }

    private EventIngestReceipt persistContentEvent(RecordContentEventCommand command) {
        Instant ingestedAt = Instant.now(clock);
        try {
            ContentEvent persisted = contentEventRepository.save(ContentEvent.record(
                    command.eventId(),
                    command.profileId(),
                    command.contentId(),
                    command.languageCode(),
                    command.eventType(),
                    command.occurredAt(),
                    ingestedAt,
                    command.sessionId(),
                    command.leftPage(),
                    command.engagementSeconds(),
                    command.metadata(),
                    command.legacyEventKey()));
            return recordedReceipt(persisted.getEventId(), EventStream.CONTENT, persisted.getIngestedAt());
        } catch (DataIntegrityViolationException exception) {
            return resolveContentDuplicate(command, exception);
        }
    }

    private EventIngestReceipt persistAppEvent(RecordAppEventCommand command) {
        Instant ingestedAt = Instant.now(clock);
        try {
            AppEvent persisted = appEventRepository.save(AppEvent.record(
                    command.eventId(),
                    command.profileId(),
                    command.eventType(),
                    command.contentId(),
                    command.occurredAt(),
                    ingestedAt,
                    command.payload(),
                    command.legacyEventKey()));
            return recordedReceipt(persisted.getEventId(), EventStream.APP, persisted.getIngestedAt());
        } catch (DataIntegrityViolationException exception) {
            return resolveAppDuplicate(command, exception);
        }
    }

    private EventIngestReceipt resolveContentDuplicate(
            RecordContentEventCommand command,
            DataIntegrityViolationException exception) {
        return contentEventRepository.findById(command.eventId())
                .map(existing -> duplicateReceipt(
                        existing.getEventId(),
                        EventStream.CONTENT,
                        EventIngestStatus.DUPLICATE_EVENT_ID,
                        existing.getIngestedAt()))
                .or(() -> command.legacyEventKey() == null
                        ? java.util.Optional.empty()
                        : contentEventRepository.findByProfileIdAndLegacyEventKey(
                                command.profileId(),
                                command.legacyEventKey())
                                .map(existing -> duplicateReceipt(
                                        existing.getEventId(),
                                        EventStream.CONTENT,
                                        EventIngestStatus.DUPLICATE_LEGACY_EVENT_KEY,
                                        existing.getIngestedAt())))
                .orElseThrow(() -> new IllegalStateException("Content event persistence failed", exception));
    }

    private EventIngestReceipt resolveAppDuplicate(
            RecordAppEventCommand command,
            DataIntegrityViolationException exception) {
        return appEventRepository.findById(command.eventId())
                .map(existing -> duplicateReceipt(
                        existing.getEventId(),
                        EventStream.APP,
                        EventIngestStatus.DUPLICATE_EVENT_ID,
                        existing.getIngestedAt()))
                .or(() -> command.legacyEventKey() == null
                        ? java.util.Optional.empty()
                        : appEventRepository.findByProfileIdAndLegacyEventKey(
                                command.profileId(),
                                command.legacyEventKey())
                                .map(existing -> duplicateReceipt(
                                        existing.getEventId(),
                                        EventStream.APP,
                                        EventIngestStatus.DUPLICATE_LEGACY_EVENT_KEY,
                                        existing.getIngestedAt())))
                .orElseThrow(() -> new IllegalStateException("App event persistence failed", exception));
    }

    private void requireContentExists(Long contentId) {
        if (contentLookupApi.findById(contentId).isEmpty()) {
            throw new ReferencedContentNotFoundException(contentId);
        }
    }

    private static EventIngestReceipt recordedReceipt(
            java.util.UUID eventId,
            EventStream stream,
            Instant ingestedAt) {
        return new EventIngestReceipt(eventId, stream, EventIngestStatus.RECORDED, ingestedAt);
    }

    private static EventIngestReceipt duplicateReceipt(
            java.util.UUID eventId,
            EventStream stream,
            EventIngestStatus status,
            Instant ingestedAt) {
        return new EventIngestReceipt(eventId, stream, status, ingestedAt);
    }
}
