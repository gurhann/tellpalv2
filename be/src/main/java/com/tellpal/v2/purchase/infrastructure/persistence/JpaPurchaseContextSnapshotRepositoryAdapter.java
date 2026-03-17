package com.tellpal.v2.purchase.infrastructure.persistence;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.tellpal.v2.purchase.domain.PurchaseContextSnapshot;
import com.tellpal.v2.purchase.domain.PurchaseContextSnapshotRepository;

@Repository
public class JpaPurchaseContextSnapshotRepositoryAdapter implements PurchaseContextSnapshotRepository {

    private final SpringDataPurchaseContextSnapshotRepository repository;

    public JpaPurchaseContextSnapshotRepositoryAdapter(SpringDataPurchaseContextSnapshotRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<PurchaseContextSnapshot> findByPurchaseEventId(Long purchaseEventId) {
        return repository.findByPurchaseEventId(purchaseEventId);
    }

    @Override
    public boolean existsByPurchaseEventId(Long purchaseEventId) {
        return repository.existsByPurchaseEventId(purchaseEventId);
    }

    @Override
    public PurchaseContextSnapshot save(PurchaseContextSnapshot purchaseContextSnapshot) {
        return repository.save(purchaseContextSnapshot);
    }
}
