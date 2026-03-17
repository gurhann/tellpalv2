package com.tellpal.v2.purchase.application;

import java.util.List;

import com.tellpal.v2.purchase.api.SubscriptionProductRecord;
import com.tellpal.v2.purchase.domain.SubscriptionProduct;

final class PurchaseCatalogMapper {

    private PurchaseCatalogMapper() {
    }

    static SubscriptionProductRecord toRecord(SubscriptionProduct subscriptionProduct) {
        return new SubscriptionProductRecord(
                subscriptionProduct.getId(),
                subscriptionProduct.getStoreCode(),
                subscriptionProduct.getProductId(),
                subscriptionProduct.getProductType(),
                subscriptionProduct.getBillingPeriodUnit(),
                subscriptionProduct.getBillingPeriodCount(),
                List.copyOf(subscriptionProduct.getEntitlementIds()),
                subscriptionProduct.isActive(),
                subscriptionProduct.getCreatedAt(),
                subscriptionProduct.getUpdatedAt());
    }
}
