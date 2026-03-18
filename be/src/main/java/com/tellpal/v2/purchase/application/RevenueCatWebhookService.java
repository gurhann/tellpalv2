package com.tellpal.v2.purchase.application;

import static com.tellpal.v2.purchase.application.RevenueCatWebhookResults.WebhookIngestStatus.DUPLICATE_EVENT_ID;
import static com.tellpal.v2.purchase.application.RevenueCatWebhookResults.WebhookIngestStatus.RECORDED;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tellpal.v2.purchase.application.PurchaseApplicationExceptions.InvalidPurchaseLookupValueException;
import com.tellpal.v2.purchase.application.PurchaseApplicationExceptions.RevenueCatAuthorizationFailedException;
import com.tellpal.v2.purchase.application.PurchaseApplicationExceptions.RevenueCatPayloadFormatException;
import com.tellpal.v2.purchase.application.RevenueCatWebhookCommands.ProcessRevenueCatWebhookCommand;
import com.tellpal.v2.purchase.application.RevenueCatWebhookResults.RevenueCatWebhookReceipt;
import com.tellpal.v2.purchase.domain.PurchaseEvent;
import com.tellpal.v2.purchase.domain.PurchaseEventRepository;
import com.tellpal.v2.purchase.domain.PurchaseSource;
import com.tellpal.v2.purchase.domain.SubscriptionProduct;
import com.tellpal.v2.purchase.domain.SubscriptionProductRepository;
import com.tellpal.v2.user.api.AppUserReference;
import com.tellpal.v2.user.api.UserLookupApi;

/**
 * Application service for validating and ingesting RevenueCat webhook events.
 *
 * <p>The service keeps the raw payload, normalizes lookup-backed fields, and deduplicates events by
 * RevenueCat event ID.
 */
@Service
public class RevenueCatWebhookService {

    private final Clock clock;
    private final PurchaseEventRepository purchaseEventRepository;
    private final SubscriptionProductRepository subscriptionProductRepository;
    private final PurchaseLookupCatalog purchaseLookupCatalog;
    private final RevenueCatAuthorizationVerifier revenueCatAuthorizationVerifier;
    private final UserLookupApi userLookupApi;

    public RevenueCatWebhookService(
            Clock clock,
            PurchaseEventRepository purchaseEventRepository,
            SubscriptionProductRepository subscriptionProductRepository,
            PurchaseLookupCatalog purchaseLookupCatalog,
            RevenueCatAuthorizationVerifier revenueCatAuthorizationVerifier,
            UserLookupApi userLookupApi) {
        this.clock = clock;
        this.purchaseEventRepository = purchaseEventRepository;
        this.subscriptionProductRepository = subscriptionProductRepository;
        this.purchaseLookupCatalog = purchaseLookupCatalog;
        this.revenueCatAuthorizationVerifier = revenueCatAuthorizationVerifier;
        this.userLookupApi = userLookupApi;
    }

    /**
     * Validates authorization and payload shape, then records one RevenueCat purchase event.
     */
    @Transactional
    public RevenueCatWebhookReceipt process(ProcessRevenueCatWebhookCommand command) {
        verifyAuthorization(command.authorizationHeader());
        Map<String, Object> eventPayload = extractEventPayload(command.payload());
        String revenueCatEventId = requireText(readString(eventPayload, "id"), "RevenueCat event ID must not be blank");

        Optional<PurchaseEvent> existingEvent = purchaseEventRepository.findByRevenuecatEventId(revenueCatEventId);
        if (existingEvent.isPresent()) {
            return new RevenueCatWebhookReceipt(existingEvent.get().getId(), DUPLICATE_EVENT_ID);
        }

        String eventTypeCode = requireCode(readString(eventPayload, "type"), "RevenueCat event type must not be blank");
        validateLookup("event type", eventTypeCode, purchaseLookupCatalog.hasActiveEventType(eventTypeCode));

        String storeCode = normalizeCode(readString(eventPayload, "store"));
        if (storeCode != null) {
            validateLookup("store", storeCode, purchaseLookupCatalog.hasActiveStore(storeCode));
        }

        String periodTypeCode = normalizeCode(readString(eventPayload, "period_type"));
        if (periodTypeCode != null) {
            validateLookup(
                    "subscription period type",
                    periodTypeCode,
                    purchaseLookupCatalog.hasActiveSubscriptionPeriodType(periodTypeCode));
        }

        String environmentCode = normalizeCode(readString(eventPayload, "environment"));
        if (environmentCode != null) {
            validateLookup(
                    "purchase environment",
                    environmentCode,
                    purchaseLookupCatalog.hasActiveEnvironment(environmentCode));
        }

        String cancelReasonCode = normalizeCode(readString(eventPayload, "cancel_reason"));
        if (cancelReasonCode != null) {
            validateLookup(
                    "cancel reason",
                    cancelReasonCode,
                    purchaseLookupCatalog.hasActiveReasonCode("CANCEL_REASON", cancelReasonCode));
        }

        String expirationReasonCode = normalizeCode(readString(eventPayload, "expiration_reason"));
        if (expirationReasonCode != null) {
            validateLookup(
                    "expiration reason",
                    expirationReasonCode,
                    purchaseLookupCatalog.hasActiveReasonCode("EXPIRATION_REASON", expirationReasonCode));
        }

        String sourceAppUserId = normalizeText(readString(eventPayload, "app_user_id"));
        String originalAppUserId = normalizeText(readString(eventPayload, "original_app_user_id"));
        List<String> aliasAppUserIds = readStringList(eventPayload, "aliases");
        AppUserReference appUserReference = resolveUser(sourceAppUserId, originalAppUserId, aliasAppUserIds)
                .orElse(null);

        String productId = normalizeText(readString(eventPayload, "product_id"));
        Long subscriptionProductId = findCatalogProductId(storeCode, productId);

        PurchaseEvent purchaseEvent = PurchaseEvent.record(
                appUserReference == null ? null : appUserReference.userId(),
                sourceAppUserId,
                originalAppUserId,
                aliasAppUserIds,
                determineOccurredAt(eventPayload),
                Instant.now(clock),
                PurchaseSource.REVENUECAT_WEBHOOK,
                eventTypeCode,
                productId,
                subscriptionProductId,
                normalizeText(readString(eventPayload, "entitlement_id")),
                readStringList(eventPayload, "entitlement_ids"),
                storeCode,
                normalizeCode(readString(eventPayload, "currency")),
                readBigDecimal(eventPayload, "price"),
                readBigDecimal(eventPayload, "price_in_purchased_currency"),
                readLong(eventPayload, "price_micros"),
                readBoolean(eventPayload, "is_trial"),
                revenueCatEventId,
                eventPayload,
                readInstantFromMillis(eventPayload, "event_timestamp_ms"),
                readInstantFromMillis(eventPayload, "expiration_at_ms"),
                readInstantFromMillis(eventPayload, "grace_period_expiration_at_ms"),
                readInstantFromMillis(eventPayload, "auto_resume_at_ms"),
                periodTypeCode,
                environmentCode,
                readBoolean(eventPayload, "is_trial_conversion"),
                cancelReasonCode,
                expirationReasonCode,
                normalizeText(readString(eventPayload, "transaction_id")),
                normalizeText(readString(eventPayload, "original_transaction_id")),
                readInteger(eventPayload, "renewal_number"),
                normalizeText(readString(eventPayload, "offer_code")),
                normalizeCode(readString(eventPayload, "country_code")),
                normalizeText(readString(eventPayload, "presented_offering_id")),
                normalizeText(readString(eventPayload, "new_product_id")),
                readBigDecimal(eventPayload, "tax_percentage"),
                readBigDecimal(eventPayload, "commission_percentage"),
                readLong(eventPayload, "net_revenue_micros"));

        PurchaseEvent savedEvent = purchaseEventRepository.save(purchaseEvent);
        return new RevenueCatWebhookReceipt(savedEvent.getId(), RECORDED);
    }

    private void verifyAuthorization(String authorizationHeader) {
        try {
            revenueCatAuthorizationVerifier.verify(authorizationHeader);
        } catch (RevenueCatAuthorizationFailedException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new RevenueCatAuthorizationFailedException("RevenueCat authorization failed", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> extractEventPayload(Map<String, Object> payload) {
        Object eventValue = payload.get("event");
        if (!(eventValue instanceof Map<?, ?> eventMap)) {
            throw new RevenueCatPayloadFormatException("RevenueCat payload must contain an event object");
        }
        LinkedHashMap<String, Object> copy = new LinkedHashMap<>();
        eventMap.forEach((key, value) -> {
            if (!(key instanceof String textKey) || textKey.isBlank()) {
                throw new RevenueCatPayloadFormatException("RevenueCat event keys must be non-blank strings");
            }
            copy.put(textKey, value);
        });
        return Collections.unmodifiableMap(copy);
    }

    private Optional<AppUserReference> resolveUser(
            String sourceAppUserId,
            String originalAppUserId,
            List<String> aliasAppUserIds) {
        ArrayList<String> candidates = new ArrayList<>();
        addCandidate(candidates, sourceAppUserId);
        addCandidate(candidates, originalAppUserId);
        aliasAppUserIds.forEach(alias -> addCandidate(candidates, alias));
        return candidates.stream()
                .map(userLookupApi::findByFirebaseUid)
                .flatMap(Optional::stream)
                .findFirst();
    }

    private Long findCatalogProductId(String storeCode, String productId) {
        if (storeCode == null || productId == null) {
            return null;
        }
        return subscriptionProductRepository.findByStoreCodeAndProductId(storeCode, productId)
                .map(SubscriptionProduct::getId)
                .orElse(null);
    }

    private Instant determineOccurredAt(Map<String, Object> eventPayload) {
        Instant purchasedAt = readInstantFromMillis(eventPayload, "purchased_at_ms");
        if (purchasedAt != null) {
            return purchasedAt;
        }
        Instant eventTimestamp = readInstantFromMillis(eventPayload, "event_timestamp_ms");
        if (eventTimestamp != null) {
            return eventTimestamp;
        }
        throw new RevenueCatPayloadFormatException("RevenueCat event must include purchased_at_ms or event_timestamp_ms");
    }

    private static void validateLookup(String lookupType, String value, boolean known) {
        if (known) {
            return;
        }
        throw new InvalidPurchaseLookupValueException(lookupType, value);
    }

    private static void addCandidate(List<String> candidates, String candidate) {
        if (candidate == null || candidates.contains(candidate)) {
            return;
        }
        candidates.add(candidate);
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new RevenueCatPayloadFormatException(message);
        }
        return value.trim();
    }

    private static String requireCode(String value, String message) {
        return requireText(value, message).toUpperCase(java.util.Locale.ROOT);
    }

    private static String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static String normalizeCode(String value) {
        String normalized = normalizeText(value);
        if (normalized == null) {
            return null;
        }
        return normalized.toUpperCase(java.util.Locale.ROOT);
    }

    private static String readString(Map<String, Object> payload, String fieldName) {
        Object value = payload.get(fieldName);
        if (value == null) {
            return null;
        }
        if (value instanceof String text) {
            return text;
        }
        throw new RevenueCatPayloadFormatException("RevenueCat field " + fieldName + " must be a string");
    }

    private static List<String> readStringList(Map<String, Object> payload, String fieldName) {
        Object value = payload.get(fieldName);
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> values)) {
            throw new RevenueCatPayloadFormatException("RevenueCat field " + fieldName + " must be an array");
        }
        ArrayList<String> copy = new ArrayList<>();
        for (Object entry : values) {
            if (!(entry instanceof String text) || text.isBlank()) {
                throw new RevenueCatPayloadFormatException(
                        "RevenueCat field " + fieldName + " must contain non-blank strings");
            }
            copy.add(text.trim());
        }
        return List.copyOf(copy);
    }

    private static Boolean readBoolean(Map<String, Object> payload, String fieldName) {
        Object value = payload.get(fieldName);
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        throw new RevenueCatPayloadFormatException("RevenueCat field " + fieldName + " must be a boolean");
    }

    private static Long readLong(Map<String, Object> payload, String fieldName) {
        Object value = payload.get(fieldName);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw new RevenueCatPayloadFormatException("RevenueCat field " + fieldName + " must be numeric");
    }

    private static Integer readInteger(Map<String, Object> payload, String fieldName) {
        Long value = readLong(payload, fieldName);
        return value == null ? null : Math.toIntExact(value);
    }

    private static BigDecimal readBigDecimal(Map<String, Object> payload, String fieldName) {
        Object value = payload.get(fieldName);
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        throw new RevenueCatPayloadFormatException("RevenueCat field " + fieldName + " must be numeric");
    }

    private static Instant readInstantFromMillis(Map<String, Object> payload, String fieldName) {
        Long value = readLong(payload, fieldName);
        return value == null ? null : Instant.ofEpochMilli(value);
    }
}
