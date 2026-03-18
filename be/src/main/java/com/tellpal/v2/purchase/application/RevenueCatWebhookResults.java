package com.tellpal.v2.purchase.application;

/**
 * Result types returned by RevenueCat webhook ingest services.
 */
public final class RevenueCatWebhookResults {

    private RevenueCatWebhookResults() {
    }

    /**
     * Describes whether a RevenueCat webhook produced a new purchase event.
     */
    public enum WebhookIngestStatus {
        RECORDED,
        DUPLICATE_EVENT_ID
    }

    /**
     * Receipt returned after ingesting one RevenueCat webhook event.
     */
    public record RevenueCatWebhookReceipt(Long purchaseEventId, WebhookIngestStatus status) {
    }
}
