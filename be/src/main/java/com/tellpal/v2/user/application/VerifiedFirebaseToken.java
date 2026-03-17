package com.tellpal.v2.user.application;

public record VerifiedFirebaseToken(String firebaseUid) {

    public VerifiedFirebaseToken {
        if (firebaseUid == null || firebaseUid.isBlank()) {
            throw new IllegalArgumentException("Firebase UID must not be blank");
        }
    }
}
