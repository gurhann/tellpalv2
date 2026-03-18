package com.tellpal.v2.purchase.application;

import com.tellpal.v2.purchase.application.RevenueCatWebhookResults.WebhookIngestStatus;

/**
 * Composite result returned after RevenueCat ingest and snapshot creation complete.
 */
public record RevenueCatWebhookProcessingResult(
        Long purchaseEventId,
        WebhookIngestStatus status,
        Long purchaseContextSnapshotId) {
}
