package com.tellpal.v2.user.domain;

import java.util.Optional;

public interface AppUserRepository {

    Optional<AppUser> findById(Long id);

    Optional<AppUser> findByFirebaseUid(String firebaseUid);

    boolean existsByFirebaseUid(String firebaseUid);

    AppUser save(AppUser appUser);
}
