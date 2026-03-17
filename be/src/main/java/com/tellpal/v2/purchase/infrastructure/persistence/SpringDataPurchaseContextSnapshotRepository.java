package com.tellpal.v2.purchase.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tellpal.v2.purchase.domain.PurchaseContextSnapshot;

interface SpringDataPurchaseContextSnapshotRepository extends JpaRepository<PurchaseContextSnapshot, Long> {

    Optional<PurchaseContextSnapshot> findByPurchaseEventId(Long purchaseEventId);

    boolean existsByPurchaseEventId(Long purchaseEventId);
}
