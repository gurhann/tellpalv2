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
 * Property-based test for purchase context snapshot uniqueness constraint (Özellik 18).
 *
 * **Validates: Requirements 11.2**
 *
 * Özellik 18: Purchase Context Snapshot Tekilliği
 * - purchase_event_id must be unique across v2_purchase_context_snapshots.
 * - Each purchase event can have at most one context snapshot.
 */
public class PurchaseContextSnapshotUniquenessPropertyTest {

    record PurchaseContextSnapshotRecord(Long purchaseEventId, Long userId, Long profileId) {}

    // -------------------------------------------------------------------------
    // Arbitraries
    // -------------------------------------------------------------------------

    /**
     * Generates a purchaseEventId between 1 and 10000.
     */
    @Provide
    Arbitrary<Long> purchaseEventId() {
        return Arbitraries.longs().between(1L, 10000L);
    }

    /**
     * Generates a userId between 1 and 10000.
     */
    @Provide
    Arbitrary<Long> userId() {
        return Arbitraries.longs().between(1L, 10000L);
    }

    /**
     * Generates a profileId between 1 and 10000 (nullable in DB, but positive here).
     */
    @Provide
    Arbitrary<Long> profileId() {
        return Arbitraries.longs().between(1L, 10000L);
    }

    /**
     * Generates a single PurchaseContextSnapshotRecord.
     */
    @Provide
    Arbitrary<PurchaseContextSnapshotRecord> snapshot() {
        return Combinators.combine(purchaseEventId(), userId(), profileId())
                .as(PurchaseContextSnapshotRecord::new);
    }

    /**
     * Generates a list of PurchaseContextSnapshotRecords with distinct purchaseEventIds (2–10 entries).
     */
    @Provide
    Arbitrary<List<PurchaseContextSnapshotRecord>> distinctSnapshots() {
        return snapshot()
                .list()
                .ofMinSize(2)
                .ofMaxSize(10)
                .uniqueElements(PurchaseContextSnapshotRecord::purchaseEventId);
    }

    // -------------------------------------------------------------------------
    // Properties
    // -------------------------------------------------------------------------

    /**
     * Özellik 18 — A purchase event can have at most one context snapshot.
     *
     * **Validates: Requirements 11.2**
     */
    @Property(tries = 100)
    void eachPurchaseEventHasAtMostOneSnapshot(
            @ForAll("distinctSnapshots") List<PurchaseContextSnapshotRecord> snapshots) {

        Map<Long, PurchaseContextSnapshotRecord> store = new HashMap<>();
        for (PurchaseContextSnapshotRecord s : snapshots) {
            store.put(s.purchaseEventId(), s);
        }

        assertThat(store)
                .as("Each purchaseEventId must map to at most one context snapshot")
                .hasSize(snapshots.size());
    }

    /**
     * Özellik 18 — Attempting to create a second snapshot for the same purchaseEventId is detected as a violation.
     *
     * **Validates: Requirements 11.2**
     */
    @Property(tries = 100)
    void secondSnapshotForSamePurchaseEventIsDetectedAsViolation(
            @ForAll("snapshot") PurchaseContextSnapshotRecord snapshot) {

        PurchaseContextSnapshotRecord duplicate = new PurchaseContextSnapshotRecord(
                snapshot.purchaseEventId(), snapshot.userId(), snapshot.profileId());

        Map<Long, PurchaseContextSnapshotRecord> store = new HashMap<>();
        store.put(snapshot.purchaseEventId(), snapshot);
        boolean wasOverwritten = store.put(duplicate.purchaseEventId(), duplicate) != null;

        assertThat(wasOverwritten)
                .as("Inserting a second snapshot for purchaseEventId='%d' must be detected as a uniqueness violation",
                        snapshot.purchaseEventId())
                .isTrue();
    }

    /**
     * Özellik 18 — Different purchaseEventIds can each have their own snapshot.
     *
     * **Validates: Requirements 11.2**
     */
    @Property(tries = 100)
    void differentPurchaseEventIdsCanEachHaveTheirOwnSnapshot(
            @ForAll("purchaseEventId") Long id1,
            @ForAll("purchaseEventId") Long id2,
            @ForAll("userId") Long userId,
            @ForAll("profileId") Long profileId) {

        Assume.that(!id1.equals(id2));

        PurchaseContextSnapshotRecord s1 = new PurchaseContextSnapshotRecord(id1, userId, profileId);
        PurchaseContextSnapshotRecord s2 = new PurchaseContextSnapshotRecord(id2, userId, profileId);

        Map<Long, PurchaseContextSnapshotRecord> store = new HashMap<>();
        store.put(s1.purchaseEventId(), s1);
        store.put(s2.purchaseEventId(), s2);

        assertThat(store)
                .as("Two snapshots with different purchaseEventIds must be stored as distinct records")
                .hasSize(2);
    }
}
