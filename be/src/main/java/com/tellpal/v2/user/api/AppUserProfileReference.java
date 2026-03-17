package com.tellpal.v2.user.api;

import java.util.List;

public record AppUserProfileReference(
        Long userId,
        Long profileId,
        String firebaseUid,
        boolean allowMarketing,
        String displayName,
        String ageRange,
        Long avatarMediaId,
        List<String> favoriteGenres,
        List<String> mainPurposes) {

    public AppUserProfileReference {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User ID must be positive");
        }
        if (profileId == null || profileId <= 0) {
            throw new IllegalArgumentException("Profile ID must be positive");
        }
        if (firebaseUid == null || firebaseUid.isBlank()) {
            throw new IllegalArgumentException("Firebase UID must not be blank");
        }
        if (ageRange == null || ageRange.isBlank()) {
            throw new IllegalArgumentException("Age range must not be blank");
        }
        favoriteGenres = favoriteGenres == null ? List.of() : List.copyOf(favoriteGenres);
        mainPurposes = mainPurposes == null ? List.of() : List.copyOf(mainPurposes);
    }
}
