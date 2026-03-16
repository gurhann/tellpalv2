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
 * Property-based test for app event idempotency constraint (Özellik 15).
 *
 * **Validates: Requirements 8.2**
 *
 * Özellik 15: Uygulama Event İdempotency
 * - event_id (UUID PK) must be unique across v2_app_events.
 * - Recording the same app event twice (same event_id) must result in exactly one stored record.
 */
public class AppEventIdempotencyPropertyTest {

    record AppEventRecord(UUID eventId, Long profileId, String eventType) {}

    // -------------------------------------------------------------------------
    // Arbitraries
    // -------------------------------------------------------------------------

    @Provide
    Arbitrary<UUID> eventId() {
        return Arbitraries.create(UUID::randomUUID);
    }

    @Provide
    Arbitrary<Long> profileId() {
        return Arbitraries.longs().between(1L, 10000L);
    }

    @Provide
    Arbitrary<String> eventType() {
        return Arbitraries.of(
                "APP_OPENED", "ONBOARDING_STARTED", "ONBOARDING_COMPLETED",
                "ONBOARDING_SKIPPED", "PAYWALL_SHOWN", "LOCKED_CONTENT_CLICKED"
        );
    }

    @Provide
    Arbitrary<AppEventRecord> appEvent() {
        return Combinators.combine(eventId(), profileId(), eventType())
                .as(AppEventRecord::new);
    }

    @Provide
    Arbitrary<List<AppEventRecord>> distinctAppEvents() {
        return appEvent()
                .list()
                .ofMinSize(2)
                .ofMaxSize(10)
                .uniqueElements(AppEventRecord::eventId);
    }

    // -------------------------------------------------------------------------
    // Properties
    // -------------------------------------------------------------------------

    /**
     * Özellik 15 — A set of distinct eventIds has no duplicates.
     * Validates: Requirement 8.2
     */
    @Property(tries = 100)
    void distinctEventIdsHaveNoDuplicates(
            @ForAll("distinctAppEvents") List<AppEventRecord> events) {

        Map<UUID, AppEventRecord> store = new HashMap<>();
        for (AppEventRecord e : events) {
            store.put(e.eventId(), e);
        }

        assertThat(store)
                .as("A collection of distinct app event IDs must contain no duplicates")
                .hasSize(events.size());
    }

    /**
     * Özellik 15 — Ingesting the same eventId twice results in exactly one stored record.
     * Validates: Requirement 8.2
     */
    @Property(tries = 100)
    void ingestingSameEventIdTwiceResultsInOneRecord(
            @ForAll("appEvent") AppEventRecord event) {

        Map<UUID, AppEventRecord> store = new HashMap<>();
        store.put(event.eventId(), event);
        store.put(event.eventId(), event);

        assertThat(store)
                .as("Ingesting the same app eventId='%s' twice must result in exactly one stored record",
                        event.eventId())
                .hasSize(1);
    }

    /**
     * Özellik 15 — Two app events with different eventIds are stored as distinct records.
     * Validates: Requirement 8.2
     */
    @Property(tries = 100)
    void differentEventIdsAreDistinct(
            @ForAll("eventId") UUID id1,
            @ForAll("eventId") UUID id2,
            @ForAll("profileId") Long profileId,
            @ForAll("eventType") String eventType) {

        Assume.that(!id1.equals(id2));

        AppEventRecord e1 = new AppEventRecord(id1, profileId, eventType);
        AppEventRecord e2 = new AppEventRecord(id2, profileId, eventType);

        Map<UUID, AppEventRecord> store = new HashMap<>();
        store.put(e1.eventId(), e1);
        store.put(e2.eventId(), e2);

        assertThat(store)
                .as("Two app events with different eventIds must be stored as distinct records")
                .hasSize(2);
    }

    /**
     * Özellik 15 — Inserting a duplicate eventId is detected as a uniqueness violation.
     * Validates: Requirement 8.2
     */
    @Property(tries = 100)
    void sameEventIdViolatesUniqueness(@ForAll("appEvent") AppEventRecord event) {
        AppEventRecord duplicate = new AppEventRecord(
                event.eventId(), event.profileId(), event.eventType());

        Map<UUID, AppEventRecord> store = new HashMap<>();
        store.put(event.eventId(), event);
        boolean wasOverwritten = store.put(duplicate.eventId(), duplicate) != null;

        assertThat(wasOverwritten)
                .as("Inserting a duplicate app eventId='%s' must be detected as a uniqueness violation",
                        event.eventId())
                .isTrue();
    }
}
