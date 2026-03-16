package com.tellpal.v2.presentation.dto.contributor;

public record ContentContributorResponse(
        Long id,
        Long contentId,
        Long contributorId,
        String role,
        String languageCode,
        String creditName,
        int sortOrder) {
}
