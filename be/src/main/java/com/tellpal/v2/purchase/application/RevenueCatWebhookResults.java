package com.tellpal.v2.purchase.application;

public final class RevenueCatWebhookResults {

    private RevenueCatWebhookResults() {
    }

    public enum WebhookIngestStatus {
        RECORDED,
        DUPLICATE_EVENT_ID
    }

    public record RevenueCatWebhookReceipt(Long purchaseEventId, WebhookIngestStatus status) {
    }
}
