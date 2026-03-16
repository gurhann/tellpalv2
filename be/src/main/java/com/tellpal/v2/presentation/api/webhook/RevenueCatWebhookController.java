package com.tellpal.v2.presentation.api.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tellpal.v2.purchase.application.PurchaseApplicationService;
import com.tellpal.v2.purchase.infrastructure.revenuecat.RevenueCatSignatureValidator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@RestController
@RequestMapping("/api/webhooks/revenuecat")
public class RevenueCatWebhookController {

    private final PurchaseApplicationService purchaseApplicationService;
    private final RevenueCatSignatureValidator signatureValidator;
    private final ObjectMapper objectMapper;

    public RevenueCatWebhookController(
            PurchaseApplicationService purchaseApplicationService,
            RevenueCatSignatureValidator signatureValidator,
            ObjectMapper objectMapper) {
        this.purchaseApplicationService = purchaseApplicationService;
        this.signatureValidator = signatureValidator;
        this.objectMapper = objectMapper;
    }

    @PostMapping(consumes = "application/json")
    public ResponseEntity<Void> handleWebhook(
            @RequestHeader(value = "X-RevenueCat-Signature", required = false) String signature,
            @RequestBody String rawPayload) {

        if (!signatureValidator.validate(rawPayload, signature)) {
            return ResponseEntity.status(401).build();
        }

        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            JsonNode event = root.path("event");

            String eventType = event.path("type").asText(null);
            String appUserId = event.path("app_user_id").asText(null);
            String revenuecatEventId = event.path("id").asText(null);
            long purchasedAtMs = event.path("purchased_at_ms").asLong(0);

            Long userId = parseUserId(appUserId);
            OffsetDateTime occurredAt = toOffsetDateTime(purchasedAtMs);
            OffsetDateTime ingestedAt = OffsetDateTime.now(ZoneOffset.UTC);

            purchaseApplicationService.recordPurchaseEvent(
                    userId, occurredAt, ingestedAt,
                    "REVENUECAT_WEBHOOK", eventType,
                    revenuecatEventId, rawPayload);

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    private Long parseUserId(String appUserId) {
        if (appUserId == null || appUserId.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(appUserId.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private OffsetDateTime toOffsetDateTime(long epochMillis) {
        if (epochMillis == 0) {
            return OffsetDateTime.now(ZoneOffset.UTC);
        }
        return Instant.ofEpochMilli(epochMillis).atOffset(ZoneOffset.UTC);
    }
}
