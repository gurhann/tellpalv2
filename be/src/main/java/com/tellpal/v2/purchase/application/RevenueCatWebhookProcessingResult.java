package com.tellpal.v2.purchase.application;

import com.tellpal.v2.purchase.application.RevenueCatWebhookResults.WebhookIngestStatus;

public record RevenueCatWebhookProcessingResult(
        Long purchaseEventId,
        WebhookIngestStatus status,
        Long purchaseContextSnapshotId) {
}
