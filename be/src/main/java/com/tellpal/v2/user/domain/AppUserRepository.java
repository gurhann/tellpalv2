package com.tellpal.v2.user.domain;

import java.util.Optional;

/**
 * Persists {@link AppUser} aggregates keyed by the Firebase uid used during authentication.
 */
public interface AppUserRepository {

    /**
     * Returns the user aggregate for the internal id when it exists.
     */
    Optional<AppUser> findById(Long id);

    /**
     * Resolves the authenticated user aggregate for the verified Firebase uid.
     */
    Optional<AppUser> findByFirebaseUid(String firebaseUid);

    /**
     * Checks whether the Firebase uid has already been mapped to an application user.
     */
    boolean existsByFirebaseUid(String firebaseUid);

    /**
     * Persists user identity or primary-profile changes.
     */
    AppUser save(AppUser appUser);
}
