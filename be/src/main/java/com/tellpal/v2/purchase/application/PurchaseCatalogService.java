package com.tellpal.v2.purchase.application;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tellpal.v2.purchase.api.PurchaseCatalogApi;
import com.tellpal.v2.purchase.api.PurchaseCatalogCommands.CreateSubscriptionProductCommand;
import com.tellpal.v2.purchase.api.PurchaseCatalogCommands.UpdateSubscriptionProductCommand;
import com.tellpal.v2.purchase.api.SubscriptionProductRecord;
import com.tellpal.v2.purchase.application.PurchaseApplicationExceptions.DuplicateSubscriptionProductException;
import com.tellpal.v2.purchase.application.PurchaseApplicationExceptions.SubscriptionProductNotFoundException;
import com.tellpal.v2.purchase.domain.SubscriptionProduct;
import com.tellpal.v2.purchase.domain.SubscriptionProductRepository;

@Service
public class PurchaseCatalogService implements PurchaseCatalogApi {

    private final SubscriptionProductRepository subscriptionProductRepository;

    public PurchaseCatalogService(SubscriptionProductRepository subscriptionProductRepository) {
        this.subscriptionProductRepository = subscriptionProductRepository;
    }

    @Override
    @Transactional
    public SubscriptionProductRecord createProduct(CreateSubscriptionProductCommand command) {
        ensureCatalogLocationAvailable(null, command.storeCode(), command.productId());
        SubscriptionProduct subscriptionProduct = SubscriptionProduct.register(
                command.storeCode(),
                command.productId(),
                command.productType(),
                command.billingPeriodUnit(),
                command.billingPeriodCount(),
                command.entitlementIds());
        return PurchaseCatalogMapper.toRecord(subscriptionProductRepository.save(subscriptionProduct));
    }

    @Override
    @Transactional
    public SubscriptionProductRecord updateProduct(UpdateSubscriptionProductCommand command) {
        SubscriptionProduct subscriptionProduct = loadProduct(command.subscriptionProductId());
        ensureCatalogLocationAvailable(command.subscriptionProductId(), command.storeCode(), command.productId());
        subscriptionProduct.renameProduct(command.storeCode(), command.productId());
        subscriptionProduct.updateCatalogDefinition(
                command.productType(),
                command.billingPeriodUnit(),
                command.billingPeriodCount(),
                command.entitlementIds());
        return PurchaseCatalogMapper.toRecord(subscriptionProductRepository.save(subscriptionProduct));
    }

    @Override
    @Transactional
    public SubscriptionProductRecord deactivateProduct(Long subscriptionProductId) {
        SubscriptionProduct subscriptionProduct = loadProduct(subscriptionProductId);
        subscriptionProduct.markActive(false);
        return PurchaseCatalogMapper.toRecord(subscriptionProductRepository.save(subscriptionProduct));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionProductRecord> listProducts() {
        return subscriptionProductRepository.findAllOrdered().stream()
                .map(PurchaseCatalogMapper::toRecord)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SubscriptionProductRecord> findById(Long subscriptionProductId) {
        if (subscriptionProductId == null || subscriptionProductId <= 0) {
            throw new IllegalArgumentException("Subscription product ID must be positive");
        }
        return subscriptionProductRepository.findById(subscriptionProductId)
                .map(PurchaseCatalogMapper::toRecord);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SubscriptionProductRecord> findByStoreAndProductId(String storeCode, String productId) {
        return subscriptionProductRepository.findByStoreCodeAndProductId(
                        normalizeStoreCode(storeCode),
                        normalizeProductId(productId))
                .map(PurchaseCatalogMapper::toRecord);
    }

    private SubscriptionProduct loadProduct(Long subscriptionProductId) {
        if (subscriptionProductId == null || subscriptionProductId <= 0) {
            throw new IllegalArgumentException("Subscription product ID must be positive");
        }
        return subscriptionProductRepository.findById(subscriptionProductId)
                .orElseThrow(() -> new SubscriptionProductNotFoundException(subscriptionProductId));
    }

    private void ensureCatalogLocationAvailable(Long currentProductId, String storeCode, String productId) {
        subscriptionProductRepository.findByStoreCodeAndProductId(storeCode, productId)
                .filter(candidate -> !candidate.getId().equals(currentProductId))
                .ifPresent(candidate -> {
                    throw new DuplicateSubscriptionProductException(storeCode, productId);
                });
    }

    private static String normalizeStoreCode(String storeCode) {
        if (storeCode == null || storeCode.isBlank()) {
            throw new IllegalArgumentException("Store code must not be blank");
        }
        return storeCode.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private static String normalizeProductId(String productId) {
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("Product ID must not be blank");
        }
        return productId.trim();
    }
}
