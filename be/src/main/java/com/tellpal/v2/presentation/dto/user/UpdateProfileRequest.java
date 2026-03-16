package com.tellpal.v2.presentation.dto.user;

import java.util.List;

public record UpdateProfileRequest(
        String name,
        Integer ageRange,
        Long avatarMediaId,
        List<String> favoriteGenres,
        List<String> mainPurposes
) {
}
