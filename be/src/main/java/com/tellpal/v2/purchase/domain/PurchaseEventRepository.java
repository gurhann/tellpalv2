package com.tellpal.v2.purchase.domain;

import java.util.Optional;

/**
 * Persists raw and normalized purchase events received from RevenueCat.
 */
public interface PurchaseEventRepository {

    /**
     * Returns the stored purchase event for the internal identifier when it exists.
     */
    Optional<PurchaseEvent> findById(Long purchaseEventId);

    /**
     * Resolves the idempotency key used to prevent duplicate processing of the same RevenueCat event.
     */
    Optional<PurchaseEvent> findByRevenuecatEventId(String revenuecatEventId);

    /**
     * Persists the processed purchase event payload and normalized fields.
     */
    PurchaseEvent save(PurchaseEvent purchaseEvent);
}
