package com.tellpal.v2.purchase.application;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collections;

/**
 * Command types used by RevenueCat webhook ingest services.
 */
public final class RevenueCatWebhookCommands {

    private RevenueCatWebhookCommands() {
    }

    /**
     * Command for processing one RevenueCat webhook request.
     */
    public record ProcessRevenueCatWebhookCommand(String authorizationHeader, Map<String, Object> payload) {

        public ProcessRevenueCatWebhookCommand {
            authorizationHeader = requireText(
                    authorizationHeader,
                    "RevenueCat authorization header must not be blank");
            payload = copyPayload(payload);
        }
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static Map<String, Object> copyPayload(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            throw new IllegalArgumentException("RevenueCat payload must not be empty");
        }
        LinkedHashMap<String, Object> copy = new LinkedHashMap<>();
        payload.forEach((key, value) -> copy.put(requireText(key, "RevenueCat payload keys must not be blank"), value));
        return Collections.unmodifiableMap(copy);
    }
}
