package com.tellpal.v2.user.api;

import java.util.Optional;

/**
 * Read-only lookup API for user and primary profile references.
 */
public interface UserLookupApi {

    /**
     * Finds a user by Firebase UID.
     */
    Optional<AppUserReference> findByFirebaseUid(String firebaseUid);

    /**
     * Finds the primary profile for a known application user.
     */
    Optional<AppUserProfileReference> findPrimaryProfileByUserId(Long userId);

    /**
     * Finds the primary profile for a Firebase UID when the user exists.
     */
    Optional<AppUserProfileReference> findPrimaryProfileByFirebaseUid(String firebaseUid);
}
