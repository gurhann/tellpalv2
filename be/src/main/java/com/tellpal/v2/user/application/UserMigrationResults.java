package com.tellpal.v2.user.application;

import java.util.List;

public final class UserMigrationResults {

    private UserMigrationResults() {
    }

    public enum FirebaseUserImportStatus {
        WOULD_CREATE,
        CREATED,
        SKIPPED_EXISTING
    }

    public record FirebaseUserImportEntry(
            String firebaseUid,
            FirebaseUserImportStatus status,
            Long userId,
            Long primaryProfileId) {

        public FirebaseUserImportEntry {
            if (firebaseUid == null || firebaseUid.isBlank()) {
                throw new IllegalArgumentException("Firebase UID must not be blank");
            }
            if (userId != null && userId <= 0) {
                throw new IllegalArgumentException("User ID must be positive when present");
            }
            if (primaryProfileId != null && primaryProfileId <= 0) {
                throw new IllegalArgumentException("Primary profile ID must be positive when present");
            }
        }
    }

    public record FirebaseUserImportSummary(
            boolean dryRun,
            int processedCount,
            int createdCount,
            int skippedCount,
            List<FirebaseUserImportEntry> entries) {

        public FirebaseUserImportSummary {
            if (processedCount < 0 || createdCount < 0 || skippedCount < 0) {
                throw new IllegalArgumentException("Import counters must not be negative");
            }
            entries = entries == null ? List.of() : List.copyOf(entries);
        }
    }
}
