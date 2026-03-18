package com.tellpal.v2.user.application;

/**
 * Verified Firebase token payload reduced to the fields needed by the user module.
 */
public record VerifiedFirebaseToken(String firebaseUid) {

    public VerifiedFirebaseToken {
        if (firebaseUid == null || firebaseUid.isBlank()) {
            throw new IllegalArgumentException("Firebase UID must not be blank");
        }
    }
}
