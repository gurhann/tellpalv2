package com.tellpal.v2.content.application;

import com.tellpal.v2.content.domain.ContributorRole;
import com.tellpal.v2.shared.domain.LanguageCode;

public final class ContributorManagementCommands {

    private ContributorManagementCommands() {
    }

    public record CreateContributorCommand(String displayName) {

        public CreateContributorCommand {
            displayName = requireText(displayName, "Contributor display name must not be blank");
        }
    }

    public record RenameContributorCommand(Long contributorId, String displayName) {

        public RenameContributorCommand {
            contributorId = requirePositiveId(contributorId, "Contributor ID must be positive");
            displayName = requireText(displayName, "Contributor display name must not be blank");
        }
    }

    public record AssignContentContributorCommand(
            Long contentId,
            Long contributorId,
            ContributorRole role,
            LanguageCode languageCode,
            String creditName,
            int sortOrder) {

        public AssignContentContributorCommand {
            contentId = requirePositiveId(contentId, "Content ID must be positive");
            contributorId = requirePositiveId(contributorId, "Contributor ID must be positive");
            role = requireRole(role);
            if (sortOrder < 0) {
                throw new IllegalArgumentException("Contributor sort order must not be negative");
            }
            if (creditName != null) {
                creditName = creditName.trim();
                if (creditName.isEmpty()) {
                    creditName = null;
                }
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
