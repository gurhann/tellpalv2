package com.tellpal.v2.purchase.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.tellpal.v2.purchase.domain.BillingPeriodUnit;
import com.tellpal.v2.purchase.domain.SubscriptionProductType;

/**
 * Command types used by the purchase catalog API.
 */
public final class PurchaseCatalogCommands {

    private PurchaseCatalogCommands() {
    }

    /**
     * Command for creating a subscription product entry in the catalog.
     */
    public record CreateSubscriptionProductCommand(
            String storeCode,
            String productId,
            SubscriptionProductType productType,
            BillingPeriodUnit billingPeriodUnit,
            Integer billingPeriodCount,
            List<String> entitlementIds) {

        public CreateSubscriptionProductCommand {
            storeCode = requireCode(storeCode, "Store code must not be blank");
            productId = requireText(productId, "Product ID must not be blank");
            productType = requireProductType(productType);
            billingPeriodUnit = requireBillingPeriodUnit(billingPeriodUnit);
            billingPeriodCount = requirePositiveInteger(billingPeriodCount, "Billing period count must be positive");
            entitlementIds = copyTextList(entitlementIds, "Entitlement IDs must not contain blank values");
        }
    }

    /**
     * Command for updating an existing subscription product entry.
     */
    public record UpdateSubscriptionProductCommand(
            Long subscriptionProductId,
            String storeCode,
            String productId,
            SubscriptionProductType productType,
            BillingPeriodUnit billingPeriodUnit,
            Integer billingPeriodCount,
            List<String> entitlementIds) {

        public UpdateSubscriptionProductCommand {
            subscriptionProductId = requirePositiveId(subscriptionProductId, "Subscription product ID must be positive");
            storeCode = requireCode(storeCode, "Store code must not be blank");
            productId = requireText(productId, "Product ID must not be blank");
            productType = requireProductType(productType);
            billingPeriodUnit = requireBillingPeriodUnit(billingPeriodUnit);
            billingPeriodCount = requirePositiveInteger(billingPeriodCount, "Billing period count must be positive");
            entitlementIds = copyTextList(entitlementIds, "Entitlement IDs must not contain blank values");
        }
    }

    private static Long requirePositiveId(Long value, String message) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static String requireCode(String value, String message) {
        return requireText(value, message).toUpperCase(Locale.ROOT);
    }

    private static Integer requirePositiveInteger(Integer value, String message) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static SubscriptionProductType requireProductType(SubscriptionProductType productType) {
        if (productType == null) {
            throw new IllegalArgumentException("Subscription product type must not be null");
        }
        return productType;
    }

    private static BillingPeriodUnit requireBillingPeriodUnit(BillingPeriodUnit billingPeriodUnit) {
        if (billingPeriodUnit == null) {
            throw new IllegalArgumentException("Billing period unit must not be null");
        }
        return billingPeriodUnit;
    }

    private static List<String> copyTextList(List<String> values, String message) {
        ArrayList<String> copy = new ArrayList<>();
        if (values == null || values.isEmpty()) {
            return copy;
        }
        values.forEach(value -> copy.add(requireText(value, message)));
        return List.copyOf(copy);
    }
}
