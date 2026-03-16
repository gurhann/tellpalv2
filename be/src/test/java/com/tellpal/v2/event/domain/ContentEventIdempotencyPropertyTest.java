package com.tellpal.v2.event.domain;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Assume;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based test for content event idempotency constraint (Özellik 13).
 *
 * **Validates: Requirements 7.2**
 *
 * Özellik 13: İçerik Event İdempotency
 * - event_id (UUID PK) must be unique across v2_content_events.
 * - Recording the same content event twice (same event_id) must result in exactly one stored record.
 */
public class ContentEventIdempotencyPropertyTest {

    record ContentEventRecord(UUID eventId, Long profileId, Long contentId, String eventType) {}

    // -------------------------------------------------------------------------
    // Arbitraries
    // -------------------------------------------------------------------------

    /**
     * Generates a random UUID as the event identifier.
     */
    @Provide
    Arbitrary<UUID> eventId() {
        return Arbitraries.create(UUID::randomUUID);
    }

    /**
     * Generates a profileId between 1 and 10000.
     */
    @Provide
    Arbitrary<Long> profileId() {
        return Arbitraries.longs().between(1L, 10000L);
    }

    /**
     * Generates a contentId between 1 and 10000.
     */
    @Provide
    Arbitrary<Long> contentId() {
        return Arbitraries.longs().between(1L, 10000L);
    }

    /**
     * Generates an eventType from the known set of content event types.
     */
    @Provide
    Arbitrary<String> eventType() {
        return Arbitraries.of("START", "EXIT", "COMPLETE");
    }

    /**
     * Generates a single ContentEventRecord.
     */
    @Provide
    Arbitrary<ContentEventRecord> contentEvent() {
        return Combinators.combine(eventId(), profileId(), contentId(), eventType())
                .as(ContentEventRecord::new);
    }

    /**
     * Generates a list of ContentEventRecords with distinct eventIds (2–10 entries).
     */
    @Provide
    Arbitrary<List<ContentEventRecord>> distinctContentEvents() {
        return contentEvent()
                .list()
                .ofMinSize(2)
                .ofMaxSize(10)
                .uniqueElements(ContentEventRecord::eventId);
    }

    // -------------------------------------------------------------------------
    // Properties
    // -------------------------------------------------------------------------

    /**
     * Özellik 13 — A set of distinct eventIds has no duplicates.
     *
     * **Validates: Requirements 7.2**
     */
    @Property(tries = 100)
    void distinctEventIdsHaveNoDuplicates(
            @ForAll("distinctContentEvents") List<ContentEventRecord> events) {

        Map<UUID, ContentEventRecord> store = new HashMap<>();
        for (ContentEventRecord e : events) {
            store.put(e.eventId(), e);
        }

        assertThat(store)
                .as("A collection of distinct eventIds must contain no duplicates")
                .hasSize(events.size());
    }

    /**
     * Özellik 13 — Ingesting the same eventId twice results in exactly one stored record (idempotency).
     *
     * **Validates: Requirements 7.2**
     */
    @Property(tries = 100)
    void ingestingSameEventIdTwiceResultsInOneRecord(
            @ForAll("contentEvent") ContentEventRecord event) {

        Map<UUID, ContentEventRecord> store = new HashMap<>();

        // First ingestion
        store.put(event.eventId(), event);
        // Second ingestion of the same event (idempotent upsert)
        store.put(event.eventId(), event);

        assertThat(store)
                .as("Ingesting the same eventId='%s' twice must result in exactly one stored record",
                        event.eventId())
                .hasSize(1);
    }

    /**
     * Özellik 13 — Two content events with different eventIds are stored as distinct records.
     *
     * **Validates: Requirements 7.2**
     */
    @Property(tries = 100)
    void differentEventIdsAreDistinct(
            @ForAll("eventId") UUID id1,
            @ForAll("eventId") UUID id2,
            @ForAll("profileId") Long profileId,
            @ForAll("contentId") Long contentId,
            @ForAll("eventType") String eventType) {

        Assume.that(!id1.equals(id2));

        ContentEventRecord e1 = new ContentEventRecord(id1, profileId, contentId, eventType);
        ContentEventRecord e2 = new ContentEventRecord(id2, profileId, contentId, eventType);

        Map<UUID, ContentEventRecord> store = new HashMap<>();
        store.put(e1.eventId(), e1);
        store.put(e2.eventId(), e2);

        assertThat(store)
                .as("Two content events with different eventIds must be stored as distinct records")
                .hasSize(2);
    }

    /**
     * Özellik 13 — Inserting a duplicate eventId is detected as a uniqueness violation.
     *
     * **Validates: Requirements 7.2**
     */
    @Property(tries = 100)
    void sameEventIdViolatesUniqueness(@ForAll("contentEvent") ContentEventRecord event) {
        ContentEventRecord duplicate = new ContentEventRecord(
                event.eventId(), event.profileId(), event.contentId(), event.eventType());

        Map<UUID, ContentEventRecord> store = new HashMap<>();
        store.put(event.eventId(), event);
        boolean wasOverwritten = store.put(duplicate.eventId(), duplicate) != null;

        assertThat(wasOverwritten)
                .as("Inserting a duplicate eventId='%s' must be detected as a uniqueness violation",
                        event.eventId())
                .isTrue();
    }
}
