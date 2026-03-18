package com.tellpal.v2.user.application;

import java.util.List;

/**
 * Command types used by user profile application services.
 */
public final class UserProfileCommands {

    private UserProfileCommands() {
    }

    /**
     * Command for updating a user profile and optionally making it primary.
     */
    public record UpdateUserProfileCommand(
            Long userId,
            Long profileId,
            String displayName,
            String ageRange,
            Long avatarMediaId,
            List<String> favoriteGenres,
            List<String> mainPurposes,
            boolean primary) {

        public UpdateUserProfileCommand {
            userId = requirePositiveId(userId, "User ID must be positive");
            profileId = requirePositiveId(profileId, "Profile ID must be positive");
            if (ageRange == null || ageRange.isBlank()) {
                throw new IllegalArgumentException("Profile age range must not be blank");
            }
            favoriteGenres = favoriteGenres == null ? List.of() : List.copyOf(favoriteGenres);
            mainPurposes = mainPurposes == null ? List.of() : List.copyOf(mainPurposes);
        }
    }

    private static Long requirePositiveId(Long value, String message) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
