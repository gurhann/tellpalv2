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
}
