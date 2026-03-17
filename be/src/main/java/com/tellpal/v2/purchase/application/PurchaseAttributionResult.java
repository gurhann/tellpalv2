package com.tellpal.v2.purchase.application;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record PurchaseAttributionResult(
        Long snapshotId,
        Long purchaseEventId,
        Long appUserId,
        Long profileId,
        int attributionWindowSeconds,
        UUID attributedAppEventId,
        Long attributedContentId,
        Map<String, Object> profileSnapshot,
        Instant createdAt) {
}
