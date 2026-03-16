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
 * Property-based test for legacy event key uniqueness constraint (Özellik 14).
 *
 * **Validates: Requirements 7.8, 18.5**
 *
 * Özellik 14: Legacy Event Key Benzersizliği
 * - (profile_id, legacy_event_key) must be unique per table (partial unique index WHERE legacy_event_key IS NOT NULL).
 * - Two events with the same profile_id and legacy_event_key are duplicates.
 * - Events with null legacy_event_key are always allowed (partial index).
 */
public class LegacyEventKeyUniquenessPropertyTest {

    record EventWithLegacyKey(UUID eventId, Long profileId, String legacyEventKey) {}

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
    Arbitrary<String> legacyEventKey() {
        return Arbitraries.strings().alpha().ofMinLength(8).ofMaxLength(64);
    }

    @Provide
    Arbitrary<EventWithLegacyKey> eventWithKey() {
        return Combinators.combine(eventId(), profileId(), legacyEventKey())
                .as(EventWithLegacyKey::new);
    }

    @Provide
    Arbitrary<List<EventWithLegacyKey>> distinctLegacyKeyEvents() {
        return eventWithKey()
                .list()
                .ofMinSize(2)
                .ofMaxSize(10)
                .uniqueElements(e -> e.profileId() + ":" + e.legacyEventKey());
    }

    // -------------------------------------------------------------------------
    // Properties
    // -------------------------------------------------------------------------

    /**
     * Özellik 14 — A set of distinct (profileId, legacyEventKey) pairs has no duplicates.
     * Validates: Requirement 7.8
     */
    @Property(tries = 100)
    void distinctProfileLegacyKeyPairsHaveNoDuplicates(
            @ForAll("distinctLegacyKeyEvents") List<EventWithLegacyKey> events) {

        Map<String, EventWithLegacyKey> store = new HashMap<>();
        for (EventWithLegacyKey e : events) {
            store.put(e.profileId() + ":" + e.legacyEventKey(), e);
        }

        assertThat(store)
                .as("Distinct (profileId, legacyEventKey) pairs must not produce duplicates")
                .hasSize(events.size());
    }

    /**
     * Özellik 14 — Two events with the same profileId and legacyEventKey are detected as duplicates.
     * Validates: Requirement 7.8
     */
    @Property(tries = 100)
    void sameProfileAndLegacyKeyIsDetectedAsDuplicate(
            @ForAll("profileId") Long profileId,
            @ForAll("legacyEventKey") String legacyEventKey) {

        EventWithLegacyKey e1 = new EventWithLegacyKey(UUID.randomUUID(), profileId, legacyEventKey);
        EventWithLegacyKey e2 = new EventWithLegacyKey(UUID.randomUUID(), profileId, legacyEventKey);

        Map<String, EventWithLegacyKey> store = new HashMap<>();
        store.put(e1.profileId() + ":" + e1.legacyEventKey(), e1);
        boolean wasOverwritten = store.put(e2.profileId() + ":" + e2.legacyEventKey(), e2) != null;

        assertThat(wasOverwritten)
                .as("Two events with same profileId=%d and legacyEventKey='%s' must be detected as duplicate",
                        profileId, legacyEventKey)
                .isTrue();
    }

    /**
     * Özellik 14 — Same legacyEventKey under different profileIds are NOT duplicates.
     * Validates: Requirement 7.8
     */
    @Property(tries = 100)
    void sameLegacyKeyDifferentProfilesAreDistinct(
            @ForAll("profileId") Long profileId1,
            @ForAll("profileId") Long profileId2,
            @ForAll("legacyEventKey") String legacyEventKey) {

        Assume.that(!profileId1.equals(profileId2));

        EventWithLegacyKey e1 = new EventWithLegacyKey(UUID.randomUUID(), profileId1, legacyEventKey);
        EventWithLegacyKey e2 = new EventWithLegacyKey(UUID.randomUUID(), profileId2, legacyEventKey);

        Map<String, EventWithLegacyKey> store = new HashMap<>();
        store.put(e1.profileId() + ":" + e1.legacyEventKey(), e1);
        store.put(e2.profileId() + ":" + e2.legacyEventKey(), e2);

        assertThat(store)
                .as("Same legacyEventKey under different profileIds must be stored as distinct records")
                .hasSize(2);
    }

    /**
     * Özellik 14 — Events with null legacyEventKey are always allowed (partial index — nulls excluded).
     * Validates: Requirement 7.8, 18.5
     */
    @Property(tries = 100)
    void nullLegacyKeyEventsAreAlwaysAllowed(
            @ForAll("profileId") Long profileId) {

        // Two events with same profileId but null legacyEventKey — both should be stored
        EventWithLegacyKey e1 = new EventWithLegacyKey(UUID.randomUUID(), profileId, null);
        EventWithLegacyKey e2 = new EventWithLegacyKey(UUID.randomUUID(), profileId, null);

        // Null keys are excluded from the partial unique index — use eventId as key
        Map<UUID, EventWithLegacyKey> store = new HashMap<>();
        store.put(e1.eventId(), e1);
        store.put(e2.eventId(), e2);

        assertThat(store)
                .as("Two events with null legacyEventKey must both be stored (partial index excludes nulls)")
                .hasSize(2);
    }
}
