package com.tellpal.v2.purchase.domain;

import com.tellpal.v2.shared.infrastructure.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "v2_purchase_events")
public class PurchaseEvent extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    @Column(name = "ingested_at", nullable = false)
    private OffsetDateTime ingestedAt;

    @Column(name = "source", nullable = false, length = 32)
    private String source;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "product_id")
    private String productId;

    @Column(name = "store", length = 32)
    private String store;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "price")
    private BigDecimal price;

    @Column(name = "price_in_purchased_currency")
    private BigDecimal priceInPurchasedCurrency;

    @Column(name = "tax_percentage")
    private BigDecimal taxPercentage;

    @Column(name = "commission_percentage")
    private BigDecimal commissionPercentage;

    @Column(name = "period_type", length = 32)
    private String periodType;

    @Column(name = "environment", length = 16)
    private String environment;

    @Column(name = "is_trial_conversion")
    private Boolean isTrialConversion;

    @Column(name = "cancel_reason", length = 32)
    private String cancelReason;

    @Column(name = "expiration_reason", length = 32)
    private String expirationReason;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "original_transaction_id")
    private String originalTransactionId;

    @Column(name = "renewal_number")
    private Integer renewalNumber;

    @Column(name = "offer_code")
    private String offerCode;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Column(name = "presented_offering_id")
    private String presentedOfferingId;

    @Column(name = "new_product_id")
    private String newProductId;

    @Column(name = "expiration_at")
    private OffsetDateTime expirationAt;

    @Column(name = "grace_period_expiration_at")
    private OffsetDateTime gracePeriodExpirationAt;

    @Column(name = "auto_resume_at")
    private OffsetDateTime autoResumeAt;

    @Column(name = "event_timestamp_at")
    private OffsetDateTime eventTimestampAt;

    @Column(name = "revenuecat_event_id", unique = true)
    private String revenuecatEventId;

    @Column(name = "raw_payload", columnDefinition = "jsonb")
    private String rawPayload;

    protected PurchaseEvent() {
    }

    public PurchaseEvent(Long userId, OffsetDateTime occurredAt, OffsetDateTime ingestedAt,
                         String source, String eventType) {
        this.userId = userId;
        this.occurredAt = occurredAt;
        this.ingestedAt = ingestedAt;
        this.source = source;
        this.eventType = eventType;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public OffsetDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(OffsetDateTime occurredAt) { this.occurredAt = occurredAt; }

    public OffsetDateTime getIngestedAt() { return ingestedAt; }
    public void setIngestedAt(OffsetDateTime ingestedAt) { this.ingestedAt = ingestedAt; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getStore() { return store; }
    public void setStore(String store) { this.store = store; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getPriceInPurchasedCurrency() { return priceInPurchasedCurrency; }
    public void setPriceInPurchasedCurrency(BigDecimal priceInPurchasedCurrency) { this.priceInPurchasedCurrency = priceInPurchasedCurrency; }

    public BigDecimal getTaxPercentage() { return taxPercentage; }
    public void setTaxPercentage(BigDecimal taxPercentage) { this.taxPercentage = taxPercentage; }

    public BigDecimal getCommissionPercentage() { return commissionPercentage; }
    public void setCommissionPercentage(BigDecimal commissionPercentage) { this.commissionPercentage = commissionPercentage; }

    public String getPeriodType() { return periodType; }
    public void setPeriodType(String periodType) { this.periodType = periodType; }

    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }

    public Boolean getIsTrialConversion() { return isTrialConversion; }
    public void setIsTrialConversion(Boolean isTrialConversion) { this.isTrialConversion = isTrialConversion; }

    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }

    public String getExpirationReason() { return expirationReason; }
    public void setExpirationReason(String expirationReason) { this.expirationReason = expirationReason; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getOriginalTransactionId() { return originalTransactionId; }
    public void setOriginalTransactionId(String originalTransactionId) { this.originalTransactionId = originalTransactionId; }

    public Integer getRenewalNumber() { return renewalNumber; }
    public void setRenewalNumber(Integer renewalNumber) { this.renewalNumber = renewalNumber; }

    public String getOfferCode() { return offerCode; }
    public void setOfferCode(String offerCode) { this.offerCode = offerCode; }

    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

    public String getPresentedOfferingId() { return presentedOfferingId; }
    public void setPresentedOfferingId(String presentedOfferingId) { this.presentedOfferingId = presentedOfferingId; }

    public String getNewProductId() { return newProductId; }
    public void setNewProductId(String newProductId) { this.newProductId = newProductId; }

    public OffsetDateTime getExpirationAt() { return expirationAt; }
    public void setExpirationAt(OffsetDateTime expirationAt) { this.expirationAt = expirationAt; }

    public OffsetDateTime getGracePeriodExpirationAt() { return gracePeriodExpirationAt; }
    public void setGracePeriodExpirationAt(OffsetDateTime gracePeriodExpirationAt) { this.gracePeriodExpirationAt = gracePeriodExpirationAt; }

    public OffsetDateTime getAutoResumeAt() { return autoResumeAt; }
    public void setAutoResumeAt(OffsetDateTime autoResumeAt) { this.autoResumeAt = autoResumeAt; }

    public OffsetDateTime getEventTimestampAt() { return eventTimestampAt; }
    public void setEventTimestampAt(OffsetDateTime eventTimestampAt) { this.eventTimestampAt = eventTimestampAt; }

    public String getRevenuecatEventId() { return revenuecatEventId; }
    public void setRevenuecatEventId(String revenuecatEventId) { this.revenuecatEventId = revenuecatEventId; }

    public String getRawPayload() { return rawPayload; }
    public void setRawPayload(String rawPayload) { this.rawPayload = rawPayload; }
}
