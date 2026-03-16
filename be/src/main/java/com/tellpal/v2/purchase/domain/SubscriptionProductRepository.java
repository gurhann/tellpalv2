package com.tellpal.v2.purchase.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubscriptionProductRepository extends JpaRepository<SubscriptionProduct, SubscriptionProductId> {

    List<SubscriptionProduct> findAllByIsActiveTrue();
}
