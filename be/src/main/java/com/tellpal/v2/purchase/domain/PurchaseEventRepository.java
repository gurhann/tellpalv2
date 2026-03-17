package com.tellpal.v2.purchase.domain;

import java.util.Optional;

public interface PurchaseEventRepository {

    Optional<PurchaseEvent> findById(Long purchaseEventId);

    Optional<PurchaseEvent> findByRevenuecatEventId(String revenuecatEventId);

    PurchaseEvent save(PurchaseEvent purchaseEvent);
}
