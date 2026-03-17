package com.tellpal.v2.user.web.mobile;

import java.util.List;

import com.tellpal.v2.user.application.UserProfileResults.UserProfileRecord;

public record MobileUserProfileResponse(
        Long userId,
        Long profileId,
        String displayName,
        String ageRange,
        Long avatarMediaId,
        List<String> favoriteGenres,
        List<String> mainPurposes,
        boolean primary) {

    static MobileUserProfileResponse from(UserProfileRecord record) {
        return new MobileUserProfileResponse(
                record.userId(),
                record.profileId(),
                record.displayName(),
                record.ageRange(),
                record.avatarMediaId(),
                record.favoriteGenres(),
                record.mainPurposes(),
                record.primary());
    }
}
