package com.tellpal.v2.event.infrastructure.firebase.migration;

import java.time.Instant;
import java.util.Map;

import com.tellpal.v2.event.domain.AppEventType;

public record LegacyFirebaseAppEventImportRecord(
        String firebaseUid,
        String legacyEventKey,
        AppEventType eventType,
        String contentExternalKey,
        Instant occurredAt,
        Map<String, Object> payload) {

    public LegacyFirebaseAppEventImportRecord {
        firebaseUid = requireText(firebaseUid, "Firebase UID must not be blank");
        legacyEventKey = requireText(legacyEventKey, "Legacy event key must not be blank");
        if (eventType == null) {
            throw new IllegalArgumentException("App event type must not be null");
        }
        if (occurredAt == null) {
            throw new IllegalArgumentException("Occurred at must not be null");
        }
        contentExternalKey = normalizeOptionalText(contentExternalKey);
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }

    private static String requireText(String value, String message) {
        String normalized = normalizeOptionalText(value);
        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private static String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
