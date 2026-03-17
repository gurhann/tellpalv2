package com.tellpal.v2.purchase.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tellpal.v2.purchase.domain.SubscriptionProduct;

interface SpringDataSubscriptionProductRepository extends JpaRepository<SubscriptionProduct, Long> {

    Optional<SubscriptionProduct> findByStoreCodeAndProductId(String storeCode, String productId);

    boolean existsByStoreCodeAndProductId(String storeCode, String productId);
}
