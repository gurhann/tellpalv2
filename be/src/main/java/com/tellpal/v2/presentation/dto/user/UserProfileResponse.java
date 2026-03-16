package com.tellpal.v2.presentation.dto.user;

import java.util.List;

public record UserProfileResponse(
        Long id,
        Long userId,
        String name,
        Integer ageRange,
        Long avatarMediaId,
        List<String> favoriteGenres,
        List<String> mainPurposes,
        boolean isPrimary
) {
}
