package com.tellpal.v2.user.api;

import java.util.Optional;

public interface UserLookupApi {

    Optional<AppUserReference> findByFirebaseUid(String firebaseUid);

    Optional<AppUserProfileReference> findPrimaryProfileByUserId(Long userId);

    Optional<AppUserProfileReference> findPrimaryProfileByFirebaseUid(String firebaseUid);
}
