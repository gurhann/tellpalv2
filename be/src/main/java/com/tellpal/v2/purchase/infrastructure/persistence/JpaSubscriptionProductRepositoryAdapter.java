package com.tellpal.v2.purchase.infrastructure.persistence;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.tellpal.v2.purchase.domain.SubscriptionProduct;
import com.tellpal.v2.purchase.domain.SubscriptionProductRepository;

@Repository
public class JpaSubscriptionProductRepositoryAdapter implements SubscriptionProductRepository {

    private final SpringDataSubscriptionProductRepository repository;

    public JpaSubscriptionProductRepositoryAdapter(SpringDataSubscriptionProductRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<SubscriptionProduct> findById(Long productId) {
        return repository.findById(productId);
    }

    @Override
    public java.util.List<SubscriptionProduct> findAllOrdered() {
        return repository.findAllByOrderByStoreCodeAscProductIdAsc();
    }

    @Override
    public Optional<SubscriptionProduct> findByStoreCodeAndProductId(String storeCode, String productId) {
        return repository.findByStoreCodeAndProductId(storeCode, productId);
    }

    @Override
    public boolean existsByStoreCodeAndProductId(String storeCode, String productId) {
        return repository.existsByStoreCodeAndProductId(storeCode, productId);
    }

    @Override
    public SubscriptionProduct save(SubscriptionProduct subscriptionProduct) {
        return repository.save(subscriptionProduct);
    }
}
