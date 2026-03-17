package com.tellpal.v2.purchase.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tellpal.v2.purchase.domain.PurchaseEvent;

interface SpringDataPurchaseEventRepository extends JpaRepository<PurchaseEvent, Long> {

    Optional<PurchaseEvent> findByRevenuecatEventId(String revenuecatEventId);
}
