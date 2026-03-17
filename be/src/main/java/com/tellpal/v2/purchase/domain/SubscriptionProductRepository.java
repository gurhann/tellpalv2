package com.tellpal.v2.purchase.domain;

import java.util.Optional;

public interface SubscriptionProductRepository {

    Optional<SubscriptionProduct> findById(Long productId);

    java.util.List<SubscriptionProduct> findAllOrdered();

    Optional<SubscriptionProduct> findByStoreCodeAndProductId(String storeCode, String productId);

    boolean existsByStoreCodeAndProductId(String storeCode, String productId);

    SubscriptionProduct save(SubscriptionProduct subscriptionProduct);
}
