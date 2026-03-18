package com.tellpal.v2.purchase.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Immutable purchase event recorded from client or RevenueCat sources.
 *
 * <p>The aggregate stores both normalized lookup-backed fields and the raw provider payload used to
 * derive them.
 */
@Entity
@Table(name = "purchase_events")
@Immutable
public class PurchaseEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "app_user_id", updatable = false)
    private Long appUserId;

    @Column(name = "source_app_user_id", length = 191, updatable = false)
    private String sourceAppUserId;

    @Column(name = "original_app_user_id", length = 191, updatable = false)
    private String originalAppUserId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "alias_app_user_ids", nullable = false, columnDefinition = "jsonb", updatable = false)
    private List<String> aliasAppUserIds = new ArrayList<>();

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "ingested_at", nullable = false, updatable = false)
    private Instant ingestedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, updatable = false, length = 32)
    private PurchaseSource source;

    @Column(name = "event_type_code", nullable = false, updatable = false, length = 64)
    private String eventTypeCode;

    @Column(name = "product_id", length = 191, updatable = false)
    private String productId;

    @Column(name = "subscription_product_id", updatable = false)
    private Long subscriptionProductId;

    @Column(name = "entitlement_id", length = 191, updatable = false)
    private String entitlementId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "entitlement_ids", nullable = false, columnDefinition = "jsonb", updatable = false)
    private List<String> entitlementIds = new ArrayList<>();

    @Column(name = "store_code", length = 32, updatable = false)
    private String storeCode;

    @Column(name = "currency_code", length = 3, updatable = false)
    private String currencyCode;

    @Column(name = "price_usd", precision = 12, scale = 4, updatable = false)
    private BigDecimal priceUsd;

    @Column(name = "price_in_purchased_currency", precision = 12, scale = 4, updatable = false)
    private BigDecimal priceInPurchasedCurrency;

    @Column(name = "price_micros", updatable = false)
    private Long priceMicros;

    @Column(name = "is_trial", updatable = false)
    private Boolean trial;

    @Column(name = "revenuecat_event_id", length = 191, updatable = false)
    private String revenuecatEventId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", nullable = false, columnDefinition = "jsonb", updatable = false)
    private Map<String, Object> rawPayload = new LinkedHashMap<>();

    @Column(name = "event_timestamp_at", updatable = false)
    private Instant eventTimestampAt;

    @Column(name = "expiration_at", updatable = false)
    private Instant expirationAt;

    @Column(name = "grace_period_expiration_at", updatable = false)
    private Instant gracePeriodExpirationAt;

    @Column(name = "auto_resume_at", updatable = false)
    private Instant autoResumeAt;

    @Column(name = "period_type_code", length = 32, updatable = false)
    private String periodTypeCode;

    @Column(name = "environment_code", length = 32, updatable = false)
    private String environmentCode;

    @Column(name = "is_trial_conversion", updatable = false)
    private Boolean trialConversion;

    @Column(name = "cancel_reason_code", length = 64, updatable = false)
    private String cancelReasonCode;

    @Column(name = "expiration_reason_code", length = 64, updatable = false)
    private String expirationReasonCode;

    @Column(name = "transaction_id", length = 191, updatable = false)
    private String transactionId;

    @Column(name = "original_transaction_id", length = 191, updatable = false)
    private String originalTransactionId;

    @Column(name = "renewal_number", updatable = false)
    private Integer renewalNumber;

    @Column(name = "offer_code", length = 191, updatable = false)
    private String offerCode;

    @Column(name = "country_code", length = 2, updatable = false)
    private String countryCode;

    @Column(name = "presented_offering_id", length = 191, updatable = false)
    private String presentedOfferingId;

    @Column(name = "new_product_id", length = 191, updatable = false)
    private String newProductId;

    @Column(name = "tax_percentage", precision = 6, scale = 5, updatable = false)
    private BigDecimal taxPercentage;

    @Column(name = "commission_percentage", precision = 6, scale = 5, updatable = false)
    private BigDecimal commissionPercentage;

    @Column(name = "net_revenue_micros", updatable = false)
    private Long netRevenueMicros;

    protected PurchaseEvent() {
    }

    private PurchaseEvent(
            Long appUserId,
            String sourceAppUserId,
            String originalAppUserId,
            List<String> aliasAppUserIds,
            Instant occurredAt,
            Instant ingestedAt,
            PurchaseSource source,
            String eventTypeCode,
            String productId,
            Long subscriptionProductId,
            String entitlementId,
            List<String> entitlementIds,
            String storeCode,
            String currencyCode,
            BigDecimal priceUsd,
            BigDecimal priceInPurchasedCurrency,
            Long priceMicros,
            Boolean trial,
            String revenuecatEventId,
            Map<String, Object> rawPayload,
            Instant eventTimestampAt,
            Instant expirationAt,
            Instant gracePeriodExpirationAt,
            Instant autoResumeAt,
            String periodTypeCode,
            String environmentCode,
            Boolean trialConversion,
            String cancelReasonCode,
            String expirationReasonCode,
            String transactionId,
            String originalTransactionId,
            Integer renewalNumber,
            String offerCode,
            String countryCode,
            String presentedOfferingId,
            String newProductId,
            BigDecimal taxPercentage,
            BigDecimal commissionPercentage,
            Long netRevenueMicros) {
        this.appUserId = PurchaseDomainValidator.normalizePositiveId(appUserId, "App user ID must be positive");
        this.sourceAppUserId = PurchaseDomainValidator.normalizeOptionalText(sourceAppUserId);
        this.originalAppUserId = PurchaseDomainValidator.normalizeOptionalText(originalAppUserId);
        this.aliasAppUserIds = PurchaseDomainValidator.copyTextList(
                aliasAppUserIds,
                "Alias app user IDs must not contain blank values");
        this.occurredAt = PurchaseDomainValidator.requireInstant(occurredAt, "Occurred at must not be null");
        this.ingestedAt = PurchaseDomainValidator.requireInstant(ingestedAt, "Ingested at must not be null");
        this.source = requireSource(source);
        this.eventTypeCode = PurchaseDomainValidator.requireCode(eventTypeCode, "Event type code must not be blank");
        this.productId = PurchaseDomainValidator.normalizeOptionalText(productId);
        this.subscriptionProductId = PurchaseDomainValidator.normalizePositiveId(
                subscriptionProductId,
                "Subscription product ID must be positive");
        this.entitlementId = PurchaseDomainValidator.normalizeOptionalText(entitlementId);
        this.entitlementIds = PurchaseDomainValidator.copyTextList(
                entitlementIds,
                "Entitlement IDs must not contain blank values");
        this.storeCode = PurchaseDomainValidator.normalizeOptionalCode(storeCode);
        this.currencyCode = PurchaseDomainValidator.normalizeCurrencyCode(currencyCode);
        this.priceUsd = priceUsd;
        this.priceInPurchasedCurrency = priceInPurchasedCurrency;
        this.priceMicros = priceMicros;
        this.trial = trial;
        this.revenuecatEventId = PurchaseDomainValidator.normalizeOptionalText(revenuecatEventId);
        this.rawPayload = PurchaseDomainValidator.copyJsonMap(rawPayload);
        this.eventTimestampAt = eventTimestampAt;
        this.expirationAt = expirationAt;
        this.gracePeriodExpirationAt = gracePeriodExpirationAt;
        this.autoResumeAt = autoResumeAt;
        this.periodTypeCode = PurchaseDomainValidator.normalizeOptionalCode(periodTypeCode);
        this.environmentCode = PurchaseDomainValidator.normalizeOptionalCode(environmentCode);
        this.trialConversion = trialConversion;
        this.cancelReasonCode = PurchaseDomainValidator.normalizeOptionalCode(cancelReasonCode);
        this.expirationReasonCode = PurchaseDomainValidator.normalizeOptionalCode(expirationReasonCode);
        this.transactionId = PurchaseDomainValidator.normalizeOptionalText(transactionId);
        this.originalTransactionId = PurchaseDomainValidator.normalizeOptionalText(originalTransactionId);
        this.renewalNumber = PurchaseDomainValidator.normalizePositiveInteger(
                renewalNumber,
                "Renewal number must be positive");
        this.offerCode = PurchaseDomainValidator.normalizeOptionalText(offerCode);
        this.countryCode = PurchaseDomainValidator.normalizeCountryCode(countryCode);
        this.presentedOfferingId = PurchaseDomainValidator.normalizeOptionalText(presentedOfferingId);
        this.newProductId = PurchaseDomainValidator.normalizeOptionalText(newProductId);
        this.taxPercentage = PurchaseDomainValidator.normalizeRatio(
                taxPercentage,
                "Tax percentage must be between 0 and 1");
        this.commissionPercentage = PurchaseDomainValidator.normalizeRatio(
                commissionPercentage,
                "Commission percentage must be between 0 and 1");
        this.netRevenueMicros = netRevenueMicros;
        validateConsistency();
    }

    /**
     * Records a new immutable purchase event.
     */
    public static PurchaseEvent record(
            Long appUserId,
            String sourceAppUserId,
            String originalAppUserId,
            List<String> aliasAppUserIds,
            Instant occurredAt,
            Instant ingestedAt,
            PurchaseSource source,
            String eventTypeCode,
            String productId,
            Long subscriptionProductId,
            String entitlementId,
            List<String> entitlementIds,
            String storeCode,
            String currencyCode,
            BigDecimal priceUsd,
            BigDecimal priceInPurchasedCurrency,
            Long priceMicros,
            Boolean trial,
            String revenuecatEventId,
            Map<String, Object> rawPayload,
            Instant eventTimestampAt,
            Instant expirationAt,
            Instant gracePeriodExpirationAt,
            Instant autoResumeAt,
            String periodTypeCode,
            String environmentCode,
            Boolean trialConversion,
            String cancelReasonCode,
            String expirationReasonCode,
            String transactionId,
            String originalTransactionId,
            Integer renewalNumber,
            String offerCode,
            String countryCode,
            String presentedOfferingId,
            String newProductId,
            BigDecimal taxPercentage,
            BigDecimal commissionPercentage,
            Long netRevenueMicros) {
        return new PurchaseEvent(
                appUserId,
                sourceAppUserId,
                originalAppUserId,
                aliasAppUserIds,
                occurredAt,
                ingestedAt,
                source,
                eventTypeCode,
                productId,
                subscriptionProductId,
                entitlementId,
                entitlementIds,
                storeCode,
                currencyCode,
                priceUsd,
                priceInPurchasedCurrency,
                priceMicros,
                trial,
                revenuecatEventId,
                rawPayload,
                eventTimestampAt,
                expirationAt,
                gracePeriodExpirationAt,
                autoResumeAt,
                periodTypeCode,
                environmentCode,
                trialConversion,
                cancelReasonCode,
                expirationReasonCode,
                transactionId,
                originalTransactionId,
                renewalNumber,
                offerCode,
                countryCode,
                presentedOfferingId,
                newProductId,
                taxPercentage,
                commissionPercentage,
                netRevenueMicros);
    }

    public Long getId() {
        return id;
    }

    public Long getAppUserId() {
        return appUserId;
    }

    public String getSourceAppUserId() {
        return sourceAppUserId;
    }

    public String getOriginalAppUserId() {
        return originalAppUserId;
    }

    public List<String> getAliasAppUserIds() {
        return List.copyOf(aliasAppUserIds);
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getIngestedAt() {
        return ingestedAt;
    }

    public PurchaseSource getSource() {
        return source;
    }

    public String getEventTypeCode() {
        return eventTypeCode;
    }

    public String getProductId() {
        return productId;
    }

    public Long getSubscriptionProductId() {
        return subscriptionProductId;
    }

    public String getEntitlementId() {
        return entitlementId;
    }

    public List<String> getEntitlementIds() {
        return List.copyOf(entitlementIds);
    }

    public String getStoreCode() {
        return storeCode;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public BigDecimal getPriceUsd() {
        return priceUsd;
    }

    public BigDecimal getPriceInPurchasedCurrency() {
        return priceInPurchasedCurrency;
    }

    public Long getPriceMicros() {
        return priceMicros;
    }

    public Boolean isTrial() {
        return trial;
    }

    public String getRevenuecatEventId() {
        return revenuecatEventId;
    }

    public Map<String, Object> getRawPayload() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(rawPayload));
    }

    public Instant getEventTimestampAt() {
        return eventTimestampAt;
    }

    public Instant getExpirationAt() {
        return expirationAt;
    }

    public Instant getGracePeriodExpirationAt() {
        return gracePeriodExpirationAt;
    }

    public Instant getAutoResumeAt() {
        return autoResumeAt;
    }

    public String getPeriodTypeCode() {
        return periodTypeCode;
    }

    public String getEnvironmentCode() {
        return environmentCode;
    }

    public Boolean isTrialConversion() {
        return trialConversion;
    }

    public String getCancelReasonCode() {
        return cancelReasonCode;
    }

    public String getExpirationReasonCode() {
        return expirationReasonCode;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getOriginalTransactionId() {
        return originalTransactionId;
    }

    public Integer getRenewalNumber() {
        return renewalNumber;
    }

    public String getOfferCode() {
        return offerCode;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getPresentedOfferingId() {
        return presentedOfferingId;
    }

    public String getNewProductId() {
        return newProductId;
    }

    public BigDecimal getTaxPercentage() {
        return taxPercentage;
    }

    public BigDecimal getCommissionPercentage() {
        return commissionPercentage;
    }

    public Long getNetRevenueMicros() {
        return netRevenueMicros;
    }

    private void validateConsistency() {
        if (source == PurchaseSource.REVENUECAT_WEBHOOK && revenuecatEventId == null) {
            throw new IllegalArgumentException("RevenueCat webhook events must include a RevenueCat event ID");
        }
        if (subscriptionProductId != null && (storeCode == null || productId == null)) {
            throw new IllegalArgumentException("Known catalog product links require store code and product ID");
        }
        if (cancelReasonCode != null && !"CANCELLATION".equals(eventTypeCode)) {
            throw new IllegalArgumentException("Cancel reason can only be recorded for CANCELLATION events");
        }
        if (expirationReasonCode != null && !"EXPIRATION".equals(eventTypeCode)) {
            throw new IllegalArgumentException("Expiration reason can only be recorded for EXPIRATION events");
        }
        if (rawPayload.isEmpty()) {
            throw new IllegalArgumentException("Raw payload must not be empty");
        }
    }

    private static PurchaseSource requireSource(PurchaseSource source) {
        if (source == null) {
            throw new IllegalArgumentException("Purchase source must not be null");
        }
        return source;
    }
}
