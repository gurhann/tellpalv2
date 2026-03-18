package com.tellpal.v2.purchase.api;

import java.util.List;
import java.util.Optional;

import com.tellpal.v2.purchase.api.PurchaseCatalogCommands.CreateSubscriptionProductCommand;
import com.tellpal.v2.purchase.api.PurchaseCatalogCommands.UpdateSubscriptionProductCommand;

/**
 * Module-facing use cases for maintaining the subscription product catalog.
 */
public interface PurchaseCatalogApi {

    /**
     * Creates a new catalog product.
     */
    SubscriptionProductRecord createProduct(CreateSubscriptionProductCommand command);

    /**
     * Updates a known catalog product.
     */
    SubscriptionProductRecord updateProduct(UpdateSubscriptionProductCommand command);

    /**
     * Marks a catalog product inactive without deleting it.
     */
    SubscriptionProductRecord deactivateProduct(Long subscriptionProductId);

    /**
     * Lists all catalog products in stable display order.
     */
    List<SubscriptionProductRecord> listProducts();

    /**
     * Finds a catalog product by ID.
     */
    Optional<SubscriptionProductRecord> findById(Long subscriptionProductId);

    /**
     * Finds a catalog product by store code and product ID.
     */
    Optional<SubscriptionProductRecord> findByStoreAndProductId(String storeCode, String productId);
}
