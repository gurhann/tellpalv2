package com.tellpal.v2.event.application;

import java.util.List;
import java.util.UUID;

public final class EventMigrationResults {

    private EventMigrationResults() {
    }

    public enum FirebaseEventImportStatus {
        WOULD_CREATE,
        CREATED,
        SKIPPED_DUPLICATE,
        SKIPPED_MISSING_PROFILE,
        SKIPPED_MISSING_CONTENT
    }

    public record FirebaseEventImportEntry(
            String legacyEventKey,
            FirebaseEventImportStatus status,
            UUID eventId) {

        public FirebaseEventImportEntry {
            if (legacyEventKey == null || legacyEventKey.isBlank()) {
                throw new IllegalArgumentException("Legacy event key must not be blank");
            }
        }
    }

    public record FirebaseEventImportSummary(
            boolean dryRun,
            int processedCount,
            int createdCount,
            int skippedCount,
            List<FirebaseEventImportEntry> entries) {

        public FirebaseEventImportSummary {
            if (processedCount < 0 || createdCount < 0 || skippedCount < 0) {
                throw new IllegalArgumentException("Import counters must not be negative");
            }
            entries = entries == null ? List.of() : List.copyOf(entries);
        }
    }
}
