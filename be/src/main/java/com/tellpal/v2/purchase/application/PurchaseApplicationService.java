package com.tellpal.v2.purchase.application;

import com.tellpal.v2.purchase.domain.PurchaseEvent;
import com.tellpal.v2.purchase.domain.PurchaseRepository;
import com.tellpal.v2.purchase.domain.SubscriptionProduct;
import com.tellpal.v2.purchase.domain.SubscriptionProductId;
import com.tellpal.v2.purchase.domain.SubscriptionProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@Transactional
public class PurchaseApplicationService {

    private final PurchaseRepository purchaseRepository;
    private final SubscriptionProductRepository subscriptionProductRepository;

    public PurchaseApplicationService(PurchaseRepository purchaseRepository,
                                      SubscriptionProductRepository subscriptionProductRepository) {
        this.purchaseRepository = purchaseRepository;
        this.subscriptionProductRepository = subscriptionProductRepository;
    }

    public PurchaseEvent recordPurchaseEvent(Long userId, OffsetDateTime occurredAt, OffsetDateTime ingestedAt,
                                             String source, String eventType, String revenuecatEventId,
                                             String rawPayload) {
        if (revenuecatEventId != null) {
            return purchaseRepository.findByRevenuecatEventId(revenuecatEventId)
                    .orElseGet(() -> createAndSavePurchaseEvent(userId, occurredAt, ingestedAt, source, eventType,
                            revenuecatEventId, rawPayload));
        }
        return createAndSavePurchaseEvent(userId, occurredAt, ingestedAt, source, eventType, null, rawPayload);
    }

    @Transactional(readOnly = true)
    public PurchaseEvent getPurchaseEvent(Long id) {
        return purchaseRepository.findById(id)
                .orElseThrow(() -> new PurchaseEventNotFoundException(id));
    }

    public SubscriptionProduct createProduct(String store, String productId, String productType) {
        SubscriptionProduct product = new SubscriptionProduct(store, productId, productType);
        return subscriptionProductRepository.save(product);
    }

    public SubscriptionProduct updateProduct(String store, String productId, String productType, Boolean isActive) {
        SubscriptionProduct product = getProduct(store, productId);
        if (productType != null) {
            product.setProductType(productType);
        }
        if (isActive != null) {
            product.setActive(isActive);
        }
        return subscriptionProductRepository.save(product);
    }

    @Transactional(readOnly = true)
    public SubscriptionProduct getProduct(String store, String productId) {
        return subscriptionProductRepository.findById(new SubscriptionProductId(store, productId))
                .orElseThrow(() -> new SubscriptionProductNotFoundException(store, productId));
    }

    @Transactional(readOnly = true)
    public List<SubscriptionProduct> listActiveProducts() {
        return subscriptionProductRepository.findAllByIsActiveTrue();
    }

    private PurchaseEvent createAndSavePurchaseEvent(Long userId, OffsetDateTime occurredAt,
                                                     OffsetDateTime ingestedAt, String source, String eventType,
                                                     String revenuecatEventId, String rawPayload) {
        PurchaseEvent event = new PurchaseEvent(userId, occurredAt, ingestedAt, source, eventType);
        event.setRevenuecatEventId(revenuecatEventId);
        event.setRawPayload(rawPayload);
        return purchaseRepository.save(event);
    }
}
