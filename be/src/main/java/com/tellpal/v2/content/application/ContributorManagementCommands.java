package com.tellpal.v2.content.application;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.tellpal.v2.content.domain.ContributorRole;
import com.tellpal.v2.shared.domain.LanguageCode;

/**
 * Command types used by contributor management application services.
 */
public final class ContributorManagementCommands {

    private ContributorManagementCommands() {
    }

    /**
     * Command for creating a contributor.
     */
    public record CreateContributorCommand(String displayName, Set<ContributorRole> roles) {

        public CreateContributorCommand {
            displayName = requireText(displayName, "Contributor display name must not be blank");
            roles = requireRoles(roles);
        }

    }

    /**
     * Command for renaming a contributor.
     */
    public record RenameContributorCommand(Long contributorId, String displayName, Set<ContributorRole> roles) {

        public RenameContributorCommand {
            contributorId = requirePositiveId(contributorId, "Contributor ID must be positive");
            displayName = requireText(displayName, "Contributor display name must not be blank");
            roles = requireRoles(roles);
        }

    }

    /**
     * Command for deleting a contributor.
     */
    public record DeleteContributorCommand(Long contributorId) {

        public DeleteContributorCommand {
            contributorId = requirePositiveId(contributorId, "Contributor ID must be positive");
        }
    }

    /**
     * Command for assigning a contributor to content.
     */
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

    /**
     * Command for removing a contributor assignment from content.
     */
    public record UnassignContentContributorCommand(
            Long contentId,
            Long contributorId,
            ContributorRole role,
            LanguageCode languageCode) {

        public UnassignContentContributorCommand {
            contentId = requirePositiveId(contentId, "Content ID must be positive");
            contributorId = requirePositiveId(contributorId, "Contributor ID must be positive");
            role = requireRole(role);
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

    public static Set<ContributorRole> requireRoles(List<ContributorRole> roles) {
        if (roles == null || roles.isEmpty() || roles.stream().anyMatch(java.util.Objects::isNull)
                || new LinkedHashSet<>(roles).size() != roles.size()) {
            throw new IllegalArgumentException("Contributor roles must contain at least one unique role");
        }
        return Set.copyOf(roles);
    }

    private static Set<ContributorRole> requireRoles(Set<ContributorRole> roles) {
        if (roles == null || roles.isEmpty() || roles.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("Contributor roles must contain at least one unique role");
        }
        return Set.copyOf(roles);
    }
}
