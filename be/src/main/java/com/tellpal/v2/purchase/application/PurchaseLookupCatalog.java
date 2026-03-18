package com.tellpal.v2.purchase.application;

/**
 * Read-only lookup port for validating normalized purchase lookup codes.
 */
public interface PurchaseLookupCatalog {

    boolean hasActiveEventType(String code);

    boolean hasActiveSubscriptionPeriodType(String code);

    boolean hasActiveStore(String code);

    boolean hasActiveEnvironment(String code);

    boolean hasActiveReasonCode(String reasonType, String code);
}
