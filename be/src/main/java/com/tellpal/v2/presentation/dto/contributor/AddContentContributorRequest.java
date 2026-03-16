package com.tellpal.v2.presentation.dto.contributor;

public record AddContentContributorRequest(
        Long contentId,
        Long contributorId,
        String role,
        String languageCode,
        String creditName,
        int sortOrder) {
}
