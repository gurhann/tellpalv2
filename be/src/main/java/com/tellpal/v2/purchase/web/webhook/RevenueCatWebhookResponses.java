package com.tellpal.v2.purchase.web.webhook;

import com.tellpal.v2.purchase.application.RevenueCatWebhookProcessingResult;

final class RevenueCatWebhookResponses {

    private RevenueCatWebhookResponses() {
    }

    static RevenueCatWebhookResponse toResponse(RevenueCatWebhookProcessingResult result) {
        return new RevenueCatWebhookResponse(
                result.purchaseEventId(),
                result.purchaseContextSnapshotId(),
                result.status().name());
    }

    record RevenueCatWebhookResponse(
            Long purchaseEventId,
            Long purchaseContextSnapshotId,
            String status) {
    }
}
