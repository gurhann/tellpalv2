package com.tellpal.v2.purchase.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class SubscriptionProductId implements Serializable {

    @Column(name = "store", nullable = false, length = 32)
    private String store;

    @Column(name = "product_id", nullable = false)
    private String productId;

    protected SubscriptionProductId() {
    }

    public SubscriptionProductId(String store, String productId) {
        this.store = store;
        this.productId = productId;
    }

    public String getStore() {
        return store;
    }

    public String getProductId() {
        return productId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SubscriptionProductId that)) return false;
        return Objects.equals(store, that.store) &&
               Objects.equals(productId, that.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(store, productId);
    }
}
