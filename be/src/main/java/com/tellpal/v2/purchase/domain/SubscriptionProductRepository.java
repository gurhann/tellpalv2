package com.tellpal.v2.purchase.domain;

import java.util.Optional;

/**
 * Persists subscription products that are exposed through the catalog API and attribution logic.
 */
public interface SubscriptionProductRepository {

    /**
     * Returns the product for the internal id when it exists.
     */
    Optional<SubscriptionProduct> findById(Long productId);

    /**
     * Returns catalog entries in their user-facing sort order.
     */
    java.util.List<SubscriptionProduct> findAllOrdered();

    /**
     * Resolves the catalog entry by the store and provider-specific product id pair.
     */
    Optional<SubscriptionProduct> findByStoreCodeAndProductId(String storeCode, String productId);

    /**
     * Checks whether the store and product-id pair is already reserved by another catalog entry.
     */
    boolean existsByStoreCodeAndProductId(String storeCode, String productId);

    /**
     * Persists a subscription product definition.
     */
    SubscriptionProduct save(SubscriptionProduct subscriptionProduct);
}
