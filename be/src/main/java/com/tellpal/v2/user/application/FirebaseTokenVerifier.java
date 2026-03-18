package com.tellpal.v2.user.application;

/**
 * Port for verifying Firebase ID tokens outside the core user use cases.
 */
public interface FirebaseTokenVerifier {

    VerifiedFirebaseToken verify(String idToken);
}
