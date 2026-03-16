package com.tellpal.v2.purchase.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PurchaseContextSnapshotRepository extends JpaRepository<PurchaseContextSnapshot, Long> {

    Optional<PurchaseContextSnapshot> findByPurchaseEventId(Long purchaseEventId);
}
