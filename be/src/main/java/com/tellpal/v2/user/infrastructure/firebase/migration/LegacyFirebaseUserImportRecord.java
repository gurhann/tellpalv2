package com.tellpal.v2.user.infrastructure.firebase.migration;

import java.util.List;

public record LegacyFirebaseUserImportRecord(
        String firebaseUid,
        boolean allowMarketing,
        LegacyFirebaseDefaultProfile defaultProfile) {

    public LegacyFirebaseUserImportRecord {
        if (firebaseUid == null || firebaseUid.isBlank()) {
            throw new IllegalArgumentException("Firebase UID must not be blank");
        }
        firebaseUid = firebaseUid.trim();
    }

    public record LegacyFirebaseDefaultProfile(
            String displayName,
            String ageRange,
            Long avatarMediaId,
            List<String> favoriteGenres,
            List<String> mainPurposes) {

        public LegacyFirebaseDefaultProfile {
            favoriteGenres = favoriteGenres == null ? List.of() : List.copyOf(favoriteGenres);
            mainPurposes = mainPurposes == null ? List.of() : List.copyOf(mainPurposes);
        }
    }
}
