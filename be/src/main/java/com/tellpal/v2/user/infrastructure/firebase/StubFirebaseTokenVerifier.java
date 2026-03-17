package com.tellpal.v2.user.infrastructure.firebase;

import com.tellpal.v2.user.application.FirebaseTokenVerifier;
import com.tellpal.v2.user.application.UserApplicationExceptions.FirebaseTokenVerificationException;
import com.tellpal.v2.user.application.VerifiedFirebaseToken;

final class StubFirebaseTokenVerifier implements FirebaseTokenVerifier {

    private static final String TOKEN_PREFIX = "stub:";

    @Override
    public VerifiedFirebaseToken verify(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new FirebaseTokenVerificationException("Firebase ID token must not be blank");
        }
        if (!idToken.startsWith(TOKEN_PREFIX)) {
            throw new FirebaseTokenVerificationException("Stub Firebase tokens must start with 'stub:'");
        }
        String firebaseUid = idToken.substring(TOKEN_PREFIX.length()).trim();
        if (firebaseUid.isEmpty()) {
            throw new FirebaseTokenVerificationException("Stub Firebase token must include a UID");
        }
        return new VerifiedFirebaseToken(firebaseUid);
    }
}
