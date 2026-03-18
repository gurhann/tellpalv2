package com.tellpal.v2.user.api;

/**
 * Module-facing use case for resolving or creating an app user from a Firebase ID token.
 */
public interface UserResolutionApi {

    /**
     * Verifies the token, creates the user when missing, and guarantees a primary profile exists.
     */
    AuthenticatedAppUser resolveOrCreateByIdToken(String idToken);
}
