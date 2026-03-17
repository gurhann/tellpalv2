package com.tellpal.v2.content.application;

import com.tellpal.v2.asset.api.AssetMediaType;
import com.tellpal.v2.shared.domain.LanguageCode;

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
