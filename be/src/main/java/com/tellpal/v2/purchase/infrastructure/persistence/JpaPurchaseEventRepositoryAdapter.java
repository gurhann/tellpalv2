package com.tellpal.v2.purchase.infrastructure.persistence;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.tellpal.v2.purchase.domain.PurchaseEvent;
import com.tellpal.v2.purchase.domain.PurchaseEventRepository;

@Repository
public class JpaPurchaseEventRepositoryAdapter implements PurchaseEventRepository {

    private final SpringDataPurchaseEventRepository repository;

    public JpaPurchaseEventRepositoryAdapter(SpringDataPurchaseEventRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<PurchaseEvent> findById(Long purchaseEventId) {
        return repository.findById(purchaseEventId);
    }

    @Override
    public Optional<PurchaseEvent> findByRevenuecatEventId(String revenuecatEventId) {
        return repository.findByRevenuecatEventId(revenuecatEventId);
    }

    @Override
    public PurchaseEvent save(PurchaseEvent purchaseEvent) {
        return repository.save(purchaseEvent);
    }
}
