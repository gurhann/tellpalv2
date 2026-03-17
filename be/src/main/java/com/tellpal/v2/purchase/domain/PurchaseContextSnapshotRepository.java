package com.tellpal.v2.purchase.domain;

import java.util.Optional;

public interface PurchaseContextSnapshotRepository {

    Optional<PurchaseContextSnapshot> findByPurchaseEventId(Long purchaseEventId);

    boolean existsByPurchaseEventId(Long purchaseEventId);

    PurchaseContextSnapshot save(PurchaseContextSnapshot purchaseContextSnapshot);
}
