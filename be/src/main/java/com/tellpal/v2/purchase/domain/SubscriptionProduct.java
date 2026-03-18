package com.tellpal.v2.purchase.domain;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.tellpal.v2.shared.infrastructure.persistence.BaseJpaEntity;

/**
 * Aggregate root for subscription product catalog entries.
 */
@Entity
@Table(name = "subscription_products")
public class SubscriptionProduct extends BaseJpaEntity {

    @Column(name = "store_code", nullable = false, length = 32)
    private String storeCode;

    @Column(name = "product_id", nullable = false, length = 191)
    private String productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false, length = 32)
    private SubscriptionProductType productType;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_period_unit", nullable = false, length = 16)
    private BillingPeriodUnit billingPeriodUnit;

    @Column(name = "billing_period_count", nullable = false)
    private Integer billingPeriodCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "entitlement_ids", nullable = false, columnDefinition = "jsonb")
    private List<String> entitlementIds = new ArrayList<>();

    @Column(name = "is_active", nullable = false)
    private boolean active;

    protected SubscriptionProduct() {
    }

    private SubscriptionProduct(
            String storeCode,
            String productId,
            SubscriptionProductType productType,
            BillingPeriodUnit billingPeriodUnit,
            Integer billingPeriodCount,
            List<String> entitlementIds,
            boolean active) {
        this.storeCode = PurchaseDomainValidator.requireCode(storeCode, "Store code must not be blank");
        this.productId = PurchaseDomainValidator.requireText(productId, "Product ID must not be blank");
        this.productType = requireProductType(productType);
        this.billingPeriodUnit = requireBillingPeriodUnit(billingPeriodUnit);
        this.billingPeriodCount = PurchaseDomainValidator.requirePositiveInteger(
                billingPeriodCount,
                "Billing period count must be positive");
        this.entitlementIds = PurchaseDomainValidator.copyTextList(
                entitlementIds,
                "Entitlement ID entries must not be blank");
        this.active = active;
    }

    /**
     * Registers a new active subscription product in the catalog.
     */
    public static SubscriptionProduct register(
            String storeCode,
            String productId,
            SubscriptionProductType productType,
            BillingPeriodUnit billingPeriodUnit,
            Integer billingPeriodCount,
            List<String> entitlementIds) {
        return new SubscriptionProduct(
                storeCode,
                productId,
                productType,
                billingPeriodUnit,
                billingPeriodCount,
                entitlementIds,
                true);
    }

    public String getStoreCode() {
        return storeCode;
    }

    public String getProductId() {
        return productId;
    }

    public SubscriptionProductType getProductType() {
        return productType;
    }

    public BillingPeriodUnit getBillingPeriodUnit() {
        return billingPeriodUnit;
    }

    public Integer getBillingPeriodCount() {
        return billingPeriodCount;
    }

    public List<String> getEntitlementIds() {
        return List.copyOf(entitlementIds);
    }

    public boolean isActive() {
        return active;
    }

    /**
     * Replaces the mutable catalog definition while keeping the product identity.
     */
    public void updateCatalogDefinition(
            SubscriptionProductType productType,
            BillingPeriodUnit billingPeriodUnit,
            Integer billingPeriodCount,
            List<String> entitlementIds) {
        this.productType = requireProductType(productType);
        this.billingPeriodUnit = requireBillingPeriodUnit(billingPeriodUnit);
        this.billingPeriodCount = PurchaseDomainValidator.requirePositiveInteger(
                billingPeriodCount,
                "Billing period count must be positive");
        this.entitlementIds = PurchaseDomainValidator.copyTextList(
                entitlementIds,
                "Entitlement ID entries must not be blank");
    }

    /**
     * Updates the store-scoped product identity fields.
     */
    public void renameProduct(String storeCode, String productId) {
        this.storeCode = PurchaseDomainValidator.requireCode(storeCode, "Store code must not be blank");
        this.productId = PurchaseDomainValidator.requireText(productId, "Product ID must not be blank");
    }

    public void markActive(boolean active) {
        this.active = active;
    }

    private static SubscriptionProductType requireProductType(SubscriptionProductType productType) {
        if (productType == null) {
            throw new IllegalArgumentException("Product type must not be null");
        }
        return productType;
    }

    private static BillingPeriodUnit requireBillingPeriodUnit(BillingPeriodUnit billingPeriodUnit) {
        if (billingPeriodUnit == null) {
            throw new IllegalArgumentException("Billing period unit must not be null");
        }
        return billingPeriodUnit;
    }
}
