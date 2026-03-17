package com.tellpal.v2.user.api;

public record AuthenticatedAppUser(
        Long userId,
        Long primaryProfileId,
        String firebaseUid,
        boolean allowMarketing) {

    public AuthenticatedAppUser {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User ID must be positive");
        }
        if (primaryProfileId == null || primaryProfileId <= 0) {
            throw new IllegalArgumentException("Primary profile ID must be positive");
        }
        if (firebaseUid == null || firebaseUid.isBlank()) {
            throw new IllegalArgumentException("Firebase UID must not be blank");
        }
    }
}
