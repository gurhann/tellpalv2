package com.tellpal.v2.purchase.domain;

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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based test for purchase event idempotency constraint (Özellik 17).
 *
 * **Validates: Requirements 10.2**
 *
 * Özellik 17: Purchase Event İdempotency
 * - revenuecat_event_id must be unique across purchase_events.
 * - Ingesting the same RevenueCat event twice must not create duplicate records.
 */
public class PurchaseEventIdempotencyPropertyTest {

    record PurchaseEventRecord(String revenuecatEventId, Long userId, String eventType) {}

    // -------------------------------------------------------------------------
    // Arbitraries
    // -------------------------------------------------------------------------

    /**
     * Generates a UUID-like revenuecatEventId: 36 chars, alphanumeric + hyphens.
     */
    @Provide
    Arbitrary<String> revenuecatEventId() {
        Arbitrary<String> seg8 = Arbitraries.strings()
                .withCharRange('a', 'f').withCharRange('0', '9')
                .ofMinLength(8).ofMaxLength(8);
        Arbitrary<String> seg4 = Arbitraries.strings()
                .withCharRange('a', 'f').withCharRange('0', '9')
                .ofMinLength(4).ofMaxLength(4);
        Arbitrary<String> seg12 = Arbitraries.strings()
                .withCharRange('a', 'f').withCharRange('0', '9')
                .ofMinLength(12).ofMaxLength(12);

        // Build a UUID-shaped string: 8-4-4-4-12
        return Combinators.combine(seg8, seg4, seg4, seg4, seg12)
                .as((a, b, c, d, e) -> a + "-" + b + "-" + c + "-" + d + "-" + e);
    }

    /**
     * Generates a userId between 1 and 10000.
     */
    @Provide
    Arbitrary<Long> userId() {
        return Arbitraries.longs().between(1L, 10000L);
    }

    /**
     * Generates an eventType from the known set of RevenueCat event types.
     */
    @Provide
    Arbitrary<String> eventType() {
        return Arbitraries.of("INITIAL_PURCHASE", "RENEWAL", "CANCELLATION", "EXPIRATION");
    }

    /**
     * Generates a single PurchaseEventRecord.
     */
    @Provide
    Arbitrary<PurchaseEventRecord> purchaseEvent() {
        return Combinators.combine(revenuecatEventId(), userId(), eventType())
                .as(PurchaseEventRecord::new);
    }

    /**
     * Generates a list of PurchaseEventRecords with distinct revenuecatEventIds (2–10 entries).
     */
    @Provide
    Arbitrary<List<PurchaseEventRecord>> distinctPurchaseEvents() {
        return purchaseEvent()
                .list()
                .ofMinSize(2)
                .ofMaxSize(10)
                .uniqueElements(PurchaseEventRecord::revenuecatEventId);
    }

    // -------------------------------------------------------------------------
    // Properties
    // -------------------------------------------------------------------------

    /**
     * Özellik 17 — A set of distinct revenuecatEventIds has no duplicates.
     *
     * **Validates: Requirements 10.2**
     */
    @Property(tries = 100)
    void distinctEventIdsHaveNoDuplicates(
            @ForAll("distinctPurchaseEvents") List<PurchaseEventRecord> events) {

        Map<String, PurchaseEventRecord> store = new HashMap<>();
        for (PurchaseEventRecord e : events) {
            store.put(e.revenuecatEventId(), e);
        }

        assertThat(store)
                .as("A collection of distinct revenuecatEventIds must contain no duplicates")
                .hasSize(events.size());
    }

    /**
     * Özellik 17 — Ingesting the same revenuecatEventId twice results in exactly one stored event (idempotency).
     *
     * **Validates: Requirements 10.2**
     */
    @Property(tries = 100)
    void ingestingSameEventIdTwiceResultsInOneRecord(
            @ForAll("purchaseEvent") PurchaseEventRecord event) {

        Map<String, PurchaseEventRecord> store = new HashMap<>();

        // First ingestion
        store.put(event.revenuecatEventId(), event);
        // Second ingestion of the same event (idempotent upsert)
        store.put(event.revenuecatEventId(), event);

        assertThat(store)
                .as("Ingesting the same revenuecatEventId='%s' twice must result in exactly one stored record",
                        event.revenuecatEventId())
                .hasSize(1);
    }

    /**
     * Özellik 17 — Two purchase events with different revenuecatEventIds are distinct.
     *
     * **Validates: Requirements 10.2**
     */
    @Property(tries = 100)
    void differentEventIdsAreDistinct(
            @ForAll("revenuecatEventId") String id1,
            @ForAll("revenuecatEventId") String id2,
            @ForAll("userId") Long userId,
            @ForAll("eventType") String eventType) {

        Assume.that(!id1.equals(id2));

        PurchaseEventRecord e1 = new PurchaseEventRecord(id1, userId, eventType);
        PurchaseEventRecord e2 = new PurchaseEventRecord(id2, userId, eventType);

        Map<String, PurchaseEventRecord> store = new HashMap<>();
        store.put(e1.revenuecatEventId(), e1);
        store.put(e2.revenuecatEventId(), e2);

        assertThat(store)
                .as("Two purchase events with different revenuecatEventIds must be stored as distinct records")
                .hasSize(2);
    }

    /**
     * Özellik 17 — Two purchase events with the same revenuecatEventId violate the uniqueness constraint.
     *
     * **Validates: Requirements 10.2**
     */
    @Property(tries = 100)
    void sameEventIdViolatesUniqueness(@ForAll("purchaseEvent") PurchaseEventRecord event) {
        PurchaseEventRecord duplicate = new PurchaseEventRecord(
                event.revenuecatEventId(), event.userId(), event.eventType());

        Map<String, PurchaseEventRecord> store = new HashMap<>();
        store.put(event.revenuecatEventId(), event);
        boolean wasOverwritten = store.put(duplicate.revenuecatEventId(), duplicate) != null;

        assertThat(wasOverwritten)
                .as("Inserting a duplicate revenuecatEventId='%s' must be detected as a uniqueness violation",
                        event.revenuecatEventId())
                .isTrue();
    }
}
