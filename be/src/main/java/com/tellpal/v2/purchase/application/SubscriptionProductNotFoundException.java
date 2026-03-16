package com.tellpal.v2.purchase.application;

public class SubscriptionProductNotFoundException extends RuntimeException {

    public SubscriptionProductNotFoundException(String store, String productId) {
        super("Subscription product not found: " + store + "/" + productId);
    }
}
