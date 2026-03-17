package com.tellpal.v2.user.application;

import com.tellpal.v2.user.api.AuthenticatedAppUser;
import com.tellpal.v2.user.domain.AppUser;
import com.tellpal.v2.user.domain.UserProfile;

final class UserApiMapper {

    private UserApiMapper() {
    }

    static AuthenticatedAppUser toAuthenticatedAppUser(AppUser appUser) {
        AppUser requiredAppUser = requireAppUser(appUser);
        UserProfile primaryProfile = requiredAppUser.primaryProfile()
                .orElseThrow(() -> new IllegalStateException("App user must have a primary profile"));
        Long userId = requiredAppUser.getId();
        Long primaryProfileId = primaryProfile.getId();
        if (userId == null || userId <= 0) {
            throw new IllegalStateException("App user must be persisted before it can be exposed");
        }
        if (primaryProfileId == null || primaryProfileId <= 0) {
            throw new IllegalStateException("Primary profile must be persisted before it can be exposed");
        }
        return new AuthenticatedAppUser(
                userId,
                primaryProfileId,
                requiredAppUser.getFirebaseUid(),
                requiredAppUser.isAllowMarketing());
    }

    private static AppUser requireAppUser(AppUser appUser) {
        if (appUser == null) {
            throw new IllegalArgumentException("App user must not be null");
        }
        return appUser;
    }
}
