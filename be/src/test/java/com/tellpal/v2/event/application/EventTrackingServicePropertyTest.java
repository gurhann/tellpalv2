package com.tellpal.v2.event.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.IntRange;

import com.tellpal.v2.content.api.ContentApiType;
import com.tellpal.v2.content.api.ContentLookupApi;
import com.tellpal.v2.content.api.ContentReference;
import com.tellpal.v2.event.api.EventTrackingCommands.RecordAppEventCommand;
import com.tellpal.v2.event.api.EventTrackingCommands.RecordContentEventCommand;
import com.tellpal.v2.event.api.EventTrackingResults.EventIngestStatus;
import com.tellpal.v2.event.domain.AppEvent;
import com.tellpal.v2.event.domain.AppEventRepository;
import com.tellpal.v2.event.domain.AppEventType;
import com.tellpal.v2.event.domain.ContentEvent;
import com.tellpal.v2.event.domain.ContentEventRepository;
import com.tellpal.v2.event.domain.ContentEventType;
import com.tellpal.v2.shared.domain.LanguageCode;

class EventTrackingServicePropertyTest {

    private static final Instant FIXED_INGESTED_AT = Instant.parse("2026-03-17T11:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INGESTED_AT, ZoneOffset.UTC);

    @Property(tries = 120)
    void sameContentEventIdIsRecordedOnlyOnce(@ForAll @IntRange(min = 1, max = 12) int repeats) {
        InMemoryContentEventRepository contentRepository = new InMemoryContentEventRepository();
        EventTrackingService service = newService(contentRepository, new InMemoryAppEventRepository());
        UUID eventId = UUID.randomUUID();

        List<EventIngestStatus> statuses = java.util.stream.IntStream.range(0, repeats)
                .mapToObj(index -> service.recordContentEvent(new RecordContentEventCommand(
                        eventId,
                        410L,
                        91L,
                        LanguageCode.TR,
                        ContentEventType.START,
                        Instant.parse("2026-03-17T10:59:00Z"),
                        null,
                        null,
                        index,
                        java.util.Map.of("attempt", index),
                        null)))
                .map(receipt -> receipt.status())
                .toList();

        assertThat(statuses.getFirst()).isEqualTo(EventIngestStatus.RECORDED);
        assertThat(statuses.stream().skip(1)).allMatch(status -> status == EventIngestStatus.DUPLICATE_EVENT_ID);
        assertThat(contentRepository.count()).isEqualTo(1);
    }

    @Property(tries = 120)
    void sameLegacyKeyWithinProfileIsRecordedOnlyOnce(
            @ForAll("distinctEventIds") List<UUID> eventIds,
            @ForAll("legacyKeys") String legacyKey) {
        InMemoryContentEventRepository contentRepository = new InMemoryContentEventRepository();
        EventTrackingService service = newService(contentRepository, new InMemoryAppEventRepository());

        List<EventIngestStatus> statuses = eventIds.stream()
                .map(eventId -> service.recordContentEvent(new RecordContentEventCommand(
                        eventId,
                        510L,
                        91L,
                        LanguageCode.EN,
                        ContentEventType.COMPLETE,
                        Instant.parse("2026-03-17T10:59:30Z"),
                        null,
                        null,
                        42,
                        java.util.Map.of("source", "property"),
                        legacyKey)))
                .map(receipt -> receipt.status())
                .toList();

        assertThat(statuses.getFirst()).isEqualTo(EventIngestStatus.RECORDED);
        assertThat(statuses.stream().skip(1)).allMatch(status -> status == EventIngestStatus.DUPLICATE_LEGACY_EVENT_KEY);
        assertThat(contentRepository.count()).isEqualTo(1);
    }

    @Provide
    Arbitrary<List<UUID>> distinctEventIds() {
        return Arbitraries.create(UUID::randomUUID)
                .list()
                .ofMinSize(2)
                .ofMaxSize(10)
                .uniqueElements();
    }

    @Provide
    Arbitrary<String> legacyKeys() {
        return Arbitraries.strings()
                .withChars("abcdefghijklmnopqrstuvwxyz0123456789-_")
                .ofMinLength(3)
                .ofMaxLength(40)
                .filter(value -> !value.isBlank());
    }

    private EventTrackingService newService(
            InMemoryContentEventRepository contentRepository,
            InMemoryAppEventRepository appRepository) {
        ContentLookupApi contentLookupApi = new ContentLookupApi() {
            @Override
            public Optional<ContentReference> findById(Long contentId) {
                return Optional.of(new ContentReference(contentId, ContentApiType.STORY, "content-" + contentId, true, 5, 0));
            }

            @Override
            public Optional<ContentReference> findByExternalKey(String externalKey) {
                return Optional.empty();
            }
        };
        return new EventTrackingService(contentRepository, appRepository, contentLookupApi, FIXED_CLOCK);
    }

    private static final class InMemoryContentEventRepository implements ContentEventRepository {

        private final LinkedHashMap<UUID, ContentEvent> events = new LinkedHashMap<>();

        @Override
        public Optional<ContentEvent> findById(UUID eventId) {
            return Optional.ofNullable(events.get(eventId));
        }

        @Override
        public Optional<ContentEvent> findByProfileIdAndLegacyEventKey(Long profileId, String legacyEventKey) {
            return events.values().stream()
                    .filter(event -> event.getProfileId().equals(profileId))
                    .filter(event -> legacyEventKey.equals(event.getLegacyEventKey()))
                    .findFirst();
        }

        @Override
        public boolean existsById(UUID eventId) {
            return events.containsKey(eventId);
        }

        @Override
        public ContentEvent save(ContentEvent contentEvent) {
            events.put(contentEvent.getEventId(), contentEvent);
            return contentEvent;
        }

        int count() {
            return events.size();
        }
    }

    private static final class InMemoryAppEventRepository implements AppEventRepository {

        private final LinkedHashMap<UUID, AppEvent> events = new LinkedHashMap<>();

        @Override
        public Optional<AppEvent> findById(UUID eventId) {
            return Optional.ofNullable(events.get(eventId));
        }

        @Override
        public Optional<AppEvent> findByProfileIdAndLegacyEventKey(Long profileId, String legacyEventKey) {
            return events.values().stream()
                    .filter(event -> event.getProfileId().equals(profileId))
                    .filter(event -> legacyEventKey.equals(event.getLegacyEventKey()))
                    .findFirst();
        }

        @Override
        public List<AppEvent> findAttributionCandidates(Long profileId, java.time.Instant occurredAfterInclusive,
                java.time.Instant occurredBeforeInclusive) {
            return events.values().stream()
                    .filter(event -> event.getProfileId().equals(profileId))
                    .filter(event -> !event.getOccurredAt().isBefore(occurredAfterInclusive))
                    .filter(event -> !event.getOccurredAt().isAfter(occurredBeforeInclusive))
                    .filter(event -> event.getEventType() == AppEventType.LOCKED_CONTENT_CLICKED
                            || event.getEventType() == AppEventType.PAYWALL_SHOWN)
                    .sorted(java.util.Comparator.comparing(AppEvent::getOccurredAt).reversed())
                    .toList();
        }

        @Override
        public boolean existsById(UUID eventId) {
            return events.containsKey(eventId);
        }

        @Override
        public AppEvent save(AppEvent appEvent) {
            events.put(appEvent.getEventId(), appEvent);
            return appEvent;
        }
    }
}
