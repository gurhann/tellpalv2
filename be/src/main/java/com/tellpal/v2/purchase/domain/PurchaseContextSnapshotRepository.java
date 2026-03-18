package com.tellpal.v2.purchase.domain;

import java.util.Optional;

/**
 * Stores normalized purchase context snapshots derived from processed purchase events.
 */
public interface PurchaseContextSnapshotRepository {

    /**
     * Returns the snapshot attached to the processed purchase event when one has been materialized.
     */
    Optional<PurchaseContextSnapshot> findByPurchaseEventId(Long purchaseEventId);

    /**
     * Checks whether snapshot generation has already happened for the purchase event.
     */
    boolean existsByPurchaseEventId(Long purchaseEventId);

    /**
     * Persists the latest normalized snapshot for downstream attribution and lookup use cases.
     */
    PurchaseContextSnapshot save(PurchaseContextSnapshot purchaseContextSnapshot);
}
