package com.tellpal.v2.event.infrastructure.firebase.migration;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.tellpal.v2.shared.domain.LanguageCode;

public record LegacyFirebaseContentEventImportRecord(
        String firebaseUid,
        String legacyEventKey,
        String contentExternalKey,
        LanguageCode languageCode,
        FirebaseContentEventType firebaseEventType,
        Instant occurredAt,
        UUID sessionId,
        Integer leftPage,
        Integer engagementSeconds,
        Map<String, Object> metadata) {

    public LegacyFirebaseContentEventImportRecord {
        firebaseUid = requireText(firebaseUid, "Firebase UID must not be blank");
        legacyEventKey = requireText(legacyEventKey, "Legacy event key must not be blank");
        contentExternalKey = requireText(contentExternalKey, "Content external key must not be blank");
        if (languageCode == null) {
            throw new IllegalArgumentException("Language code must not be null");
        }
        if (firebaseEventType == null) {
            throw new IllegalArgumentException("Firebase content event type must not be null");
        }
        if (occurredAt == null) {
            throw new IllegalArgumentException("Occurred at must not be null");
        }
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public enum FirebaseContentEventType {
        START_CONTENT,
        LEFT_CONTENT,
        FINISH_CONTENT
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
