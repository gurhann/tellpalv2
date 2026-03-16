package com.tellpal.v2.purchase.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "v2_subscription_products")
public class SubscriptionProduct {

    @EmbeddedId
    private SubscriptionProductId id;

    @Column(name = "product_type", nullable = false, length = 32)
    private String productType;

    @Column(name = "billing_period_unit", length = 16)
    private String billingPeriodUnit;

    @Column(name = "billing_period_count")
    private Integer billingPeriodCount;

    @Column(name = "entitlement_ids", columnDefinition = "jsonb", nullable = false)
    private String entitlementIds = "[]";

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected SubscriptionProduct() {
    }

    public SubscriptionProduct(String store, String productId, String productType) {
        this.id = new SubscriptionProductId(store, productId);
        this.productType = productType;
    }

    public SubscriptionProductId getId() {
        return id;
    }

    public String getStore() {
        return id.getStore();
    }

    public String getProductId() {
        return id.getProductId();
    }

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public String getBillingPeriodUnit() {
        return billingPeriodUnit;
    }

    public void setBillingPeriodUnit(String billingPeriodUnit) {
        this.billingPeriodUnit = billingPeriodUnit;
    }

    public Integer getBillingPeriodCount() {
        return billingPeriodCount;
    }

    public void setBillingPeriodCount(Integer billingPeriodCount) {
        this.billingPeriodCount = billingPeriodCount;
    }

    public String getEntitlementIds() {
        return entitlementIds;
    }

    public void setEntitlementIds(String entitlementIds) {
        this.entitlementIds = entitlementIds;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
