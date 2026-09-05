package com.tellpal.v2.content.application;

import com.tellpal.v2.asset.api.AssetMediaType;
import com.tellpal.v2.content.domain.ContributorRole;
import com.tellpal.v2.shared.domain.LanguageCode;

/**
 * Application-layer exceptions raised by content use cases.
 */
public final class ContentApplicationExceptions {

    private ContentApplicationExceptions() {
    }

    public static final class DuplicateContentExternalKeyException extends RuntimeException {

        public DuplicateContentExternalKeyException(String externalKey) {
            super("Content external key already exists: " + externalKey);
        }
    }

    public static final class ContentNotFoundException extends RuntimeException {

        public ContentNotFoundException(Long contentId) {
            super("Content not found: " + contentId);
        }
    }

    public static final class ContentLocalizationAlreadyExistsException extends RuntimeException {

        public ContentLocalizationAlreadyExistsException(Long contentId, LanguageCode languageCode) {
            super("Content localization already exists for content " + contentId + " and language " + languageCode);
        }
    }

    public static final class ContentLocalizationNotFoundException extends RuntimeException {

        public ContentLocalizationNotFoundException(Long contentId, LanguageCode languageCode) {
            super("Content localization not found for content " + contentId + " and language " + languageCode);
        }
    }

    public static final class StoryPageNotFoundException extends RuntimeException {

        public StoryPageNotFoundException(Long contentId, int pageNumber) {
            super("Story page not found for content " + contentId + " and page " + pageNumber);
        }
    }

    public static final class StoryPageTextlessIllustrationsMissingException extends RuntimeException {

        public StoryPageTextlessIllustrationsMissingException(Long contentId) {
            super("No textless story page illustrations are available for content " + contentId);
        }
    }

    public static final class AssetReferenceNotFoundException extends RuntimeException {

        public AssetReferenceNotFoundException(String fieldName, Long assetId) {
            super("Asset not found for " + fieldName + ": " + assetId);
        }
    }

    public static final class AssetMediaTypeMismatchException extends RuntimeException {

        public AssetMediaTypeMismatchException(
                String fieldName,
                Long assetId,
                AssetMediaType expectedMediaType,
                AssetMediaType actualMediaType) {
            super("Asset " + assetId + " for " + fieldName + " must be "
                    + expectedMediaType + " but was " + actualMediaType);
        }
    }

    public static final class ContributorNotFoundException extends RuntimeException {

        public ContributorNotFoundException(Long contributorId) {
            super("Contributor not found: " + contributorId);
        }
    }

    public static final class ContentContributorNotFoundException extends RuntimeException {

        public ContentContributorNotFoundException(Long contentId, Long contributorId, ContributorRole role,
                LanguageCode languageCode) {
            super("Content contributor assignment not found for content "
                    + contentId
                    + ", contributor "
                    + contributorId
                    + ", role "
                    + role
                    + " and language "
                    + (languageCode == null ? "global" : languageCode.value()));
        }
    }

    public static final class ContentContributorAssignmentNotFoundException extends RuntimeException {

        public ContentContributorAssignmentNotFoundException(Long contentId, Long assignmentId) {
            super("Content contributor assignment not found for content " + contentId + " and assignment " + assignmentId);
        }
    }

    public static final class ContributorRoleNotSupportedException extends RuntimeException {
        private final ContributorRole role;

        public ContributorRoleNotSupportedException(ContributorRole role) {
            super("Contributor does not have the requested role: " + role);
            this.role = role;
        }

        public ContributorRole getRole() { return role; }
    }

    public static final class ContributorAssignmentLanguageNotFoundException extends RuntimeException {
        private final LanguageCode languageCode;

        public ContributorAssignmentLanguageNotFoundException(LanguageCode languageCode) {
            super("Contributor assignment language must exist on content: " + languageCode);
            this.languageCode = languageCode;
        }

        public LanguageCode getLanguageCode() { return languageCode; }
    }

    public static final class DuplicateContributorAssignmentException extends RuntimeException {
        private final ContributorRole role;
        private final LanguageCode languageCode;

        public DuplicateContributorAssignmentException(ContributorRole role, LanguageCode languageCode) {
            super("Contributor assignment already exists for role " + role + " and language "
                    + (languageCode == null ? "global" : languageCode.value()));
            this.role = role;
            this.languageCode = languageCode;
        }

        public ContributorRole getRole() { return role; }
        public LanguageCode getLanguageCode() { return languageCode; }
    }

    public static final class ContributorInUseException extends RuntimeException {

        public ContributorInUseException(Long contributorId) {
            super("Contributor is still assigned to content and cannot be deleted: " + contributorId);
        }
    }

    public static final class DuplicateContributorDisplayNameException extends RuntimeException {
        private final Long existingContributorId;

        public DuplicateContributorDisplayNameException(Long existingContributorId) {
            super("Contributor display name already belongs to contributor: " + existingContributorId);
            this.existingContributorId = existingContributorId;
        }

        public Long getExistingContributorId() { return existingContributorId; }
    }

    public static final class ContributorRoleInUseException extends RuntimeException {
        private final ContributorRole role;
        private final long usageCount;
        private final java.util.List<com.tellpal.v2.content.domain.ContentRepository.ContributorRoleUsage> affectedContents;

        public ContributorRoleInUseException(ContributorRole role, long usageCount,
                java.util.List<com.tellpal.v2.content.domain.ContentRepository.ContributorRoleUsage> affectedContents) {
            super("Contributor role is still assigned to content: " + role);
            this.role = role;
            this.usageCount = usageCount;
            this.affectedContents = java.util.List.copyOf(affectedContents);
        }

        public ContributorRole getRole() { return role; }
        public long getUsageCount() { return usageCount; }
        public java.util.List<com.tellpal.v2.content.domain.ContentRepository.ContributorRoleUsage> getAffectedContents() { return affectedContents; }
    }

    public static final class ContentFreeAccessAlreadyExistsException extends RuntimeException {

        public ContentFreeAccessAlreadyExistsException(String accessKey, Long contentId, LanguageCode languageCode) {
            super("Content free-access entry already exists for key "
                    + accessKey
                    + ", content "
                    + contentId
                    + " and language "
                    + languageCode.value());
        }
    }

    public static final class ContentFreeAccessNotFoundException extends RuntimeException {

        public ContentFreeAccessNotFoundException(String accessKey, Long contentId, LanguageCode languageCode) {
            super("Content free-access entry not found for key "
                    + accessKey
                    + ", content "
                    + contentId
                    + " and language "
                    + languageCode.value());
        }
    }
}
