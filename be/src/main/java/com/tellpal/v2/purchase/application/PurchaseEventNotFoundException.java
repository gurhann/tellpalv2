package com.tellpal.v2.purchase.application;

public class PurchaseEventNotFoundException extends RuntimeException {

    public PurchaseEventNotFoundException(Long id) {
        super("Purchase event not found: " + id);
    }
}
