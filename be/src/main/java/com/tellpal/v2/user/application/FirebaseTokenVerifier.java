package com.tellpal.v2.user.application;

public interface FirebaseTokenVerifier {

    VerifiedFirebaseToken verify(String idToken);
}
