package com.tellpal.v2.content.application;

import com.tellpal.v2.content.domain.ContributorRole;
import com.tellpal.v2.shared.domain.LanguageCode;

public final class ContributorManagementResults {

    private ContributorManagementResults() {
    }

    public record ContributorRecord(Long contributorId, String displayName) {

        public ContributorRecord {
            contributorId = requirePositiveId(contributorId, "Contributor ID must be positive");
            displayName = requireText(displayName, "Contributor display name must not be blank");
        }
    }

    public record ContentContributorRecord(
            Long contentId,
            Long contributorId,
            String contributorDisplayName,
            ContributorRole role,
            LanguageCode languageCode,
            String creditName,
            int sortOrder) {

        public ContentContributorRecord {
            contentId = requirePositiveId(contentId, "Content ID must be positive");
            contributorId = requirePositiveId(contributorId, "Contributor ID must be positive");
            contributorDisplayName = requireText(
                    contributorDisplayName,
                    "Contributor display name must not be blank");
            role = requireRole(role);
            if (sortOrder < 0) {
                throw new IllegalArgumentException("Contributor sort order must not be negative");
            }
        }
    }

    private static Long requirePositiveId(Long value, String message) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static ContributorRole requireRole(ContributorRole role) {
        if (role == null) {
            throw new IllegalArgumentException("Contributor role must not be null");
        }
        return role;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
