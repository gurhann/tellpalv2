package com.tellpal.v2.purchase.api;

import java.time.Instant;
import java.util.List;

import com.tellpal.v2.purchase.domain.BillingPeriodUnit;
import com.tellpal.v2.purchase.domain.SubscriptionProductType;

/**
 * Snapshot of a subscription product as exposed outside the purchase module.
 */
public record SubscriptionProductRecord(
        Long id,
        String storeCode,
        String productId,
        SubscriptionProductType productType,
        BillingPeriodUnit billingPeriodUnit,
        Integer billingPeriodCount,
        List<String> entitlementIds,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {
}
