package com.tellpal.v2.purchase.application;

public final class PurchaseApplicationExceptions {

    private PurchaseApplicationExceptions() {
    }

    public static final class DuplicateSubscriptionProductException extends RuntimeException {

        public DuplicateSubscriptionProductException(String storeCode, String productId) {
            super("Subscription product already exists for store " + storeCode + " and product " + productId);
        }
    }

    public static final class SubscriptionProductNotFoundException extends RuntimeException {

        public SubscriptionProductNotFoundException(Long subscriptionProductId) {
            super("Subscription product not found: " + subscriptionProductId);
        }
    }

    public static final class InvalidPurchaseLookupValueException extends RuntimeException {

        public InvalidPurchaseLookupValueException(String lookupType, String value) {
            super("Unknown " + lookupType + " value: " + value);
        }
    }

    public static final class RevenueCatAuthorizationFailedException extends RuntimeException {

        public RevenueCatAuthorizationFailedException(String message) {
            super(message);
        }

        public RevenueCatAuthorizationFailedException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static final class RevenueCatPayloadFormatException extends RuntimeException {

        public RevenueCatPayloadFormatException(String message) {
            super(message);
        }
    }
}
