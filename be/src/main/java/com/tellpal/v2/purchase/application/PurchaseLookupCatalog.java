package com.tellpal.v2.purchase.application;

public interface PurchaseLookupCatalog {

    boolean hasActiveEventType(String code);

    boolean hasActiveSubscriptionPeriodType(String code);

    boolean hasActiveStore(String code);

    boolean hasActiveEnvironment(String code);

    boolean hasActiveReasonCode(String reasonType, String code);
}
