package com.tellpal.v2.user.application;

import com.tellpal.v2.user.application.UserProfileResults.UserProfileRecord;
import com.tellpal.v2.user.domain.UserProfile;

final class UserProfileMapper {

    private UserProfileMapper() {
    }

    static UserProfileRecord toRecord(Long userId, UserProfile profile) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User ID must be positive");
        }
        if (profile == null || profile.getId() == null || profile.getId() <= 0) {
            throw new IllegalArgumentException("User profile must be persisted");
        }
        return new UserProfileRecord(
                userId,
                profile.getId(),
                profile.getDisplayName(),
                profile.getAgeRange(),
                profile.getAvatarMediaId(),
                profile.getFavoriteGenres(),
                profile.getMainPurposes(),
                profile.isPrimary());
    }
}
