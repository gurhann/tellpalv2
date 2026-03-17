package com.tellpal.v2.purchase.api;

import java.util.List;
import java.util.Optional;

import com.tellpal.v2.purchase.api.PurchaseCatalogCommands.CreateSubscriptionProductCommand;
import com.tellpal.v2.purchase.api.PurchaseCatalogCommands.UpdateSubscriptionProductCommand;

public interface PurchaseCatalogApi {

    SubscriptionProductRecord createProduct(CreateSubscriptionProductCommand command);

    SubscriptionProductRecord updateProduct(UpdateSubscriptionProductCommand command);

    SubscriptionProductRecord deactivateProduct(Long subscriptionProductId);

    List<SubscriptionProductRecord> listProducts();

    Optional<SubscriptionProductRecord> findById(Long subscriptionProductId);

    Optional<SubscriptionProductRecord> findByStoreAndProductId(String storeCode, String productId);
}
