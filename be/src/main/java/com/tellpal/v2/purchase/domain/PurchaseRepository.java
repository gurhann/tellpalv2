package com.tellpal.v2.purchase.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PurchaseRepository extends JpaRepository<PurchaseEvent, Long> {

    Optional<PurchaseEvent> findByRevenuecatEventId(String revenuecatEventId);
}
