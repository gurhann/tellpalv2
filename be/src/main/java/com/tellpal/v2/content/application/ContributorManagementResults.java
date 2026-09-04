package com.tellpal.v2.content.application;

import java.util.Set;

import com.tellpal.v2.content.domain.ContributorRole;
import com.tellpal.v2.shared.domain.LanguageCode;

/**
 * Result types returned by contributor management application services.
 */
public final class ContributorManagementResults {

    private ContributorManagementResults() {
    }

    /**
     * Snapshot of one contributor after a management operation.
     */
    public record ContributorRecord(Long contributorId, String displayName, Set<ContributorRole> roles) {

        public ContributorRecord {
            contributorId = requirePositiveId(contributorId, "Contributor ID must be positive");
            displayName = requireText(displayName, "Contributor display name must not be blank");
            roles = Set.copyOf(roles);
        }

        public ContributorRecord(Long contributorId, String displayName) {
            this(contributorId, displayName, Set.of(ContributorRole.AUTHOR));
        }
    }

    /**
     * Snapshot of one contributor assignment to content.
     */
    public record ContentContributorRecord(
            Long assignmentId,
            Long contentId,
            Long contributorId,
            String contributorDisplayName,
            ContributorRole role,
            LanguageCode languageCode,
            String creditName,
            int sortOrder) {

        public ContentContributorRecord {
            assignmentId = requirePositiveId(assignmentId, "Contributor assignment ID must be positive");
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
