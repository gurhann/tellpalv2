package com.tellpal.v2.user.application;

import java.util.List;

/**
 * Result types returned by user profile application services.
 */
public final class UserProfileResults {

    private UserProfileResults() {
    }

    /**
     * Snapshot of one user profile after a read or update operation.
     */
    public record UserProfileRecord(
            Long userId,
            Long profileId,
            String displayName,
            String ageRange,
            Long avatarMediaId,
            List<String> favoriteGenres,
            List<String> mainPurposes,
            boolean primary) {

        public UserProfileRecord {
            if (userId == null || userId <= 0) {
                throw new IllegalArgumentException("User ID must be positive");
            }
            if (profileId == null || profileId <= 0) {
                throw new IllegalArgumentException("Profile ID must be positive");
            }
            if (ageRange == null || ageRange.isBlank()) {
                throw new IllegalArgumentException("Profile age range must not be blank");
            }
            favoriteGenres = favoriteGenres == null ? List.of() : List.copyOf(favoriteGenres);
            mainPurposes = mainPurposes == null ? List.of() : List.copyOf(mainPurposes);
        }
    }
}
