package com.tellpal.v2.category.application;

import com.tellpal.v2.asset.api.AssetMediaType;
import com.tellpal.v2.shared.domain.LanguageCode;

/**
 * Application-layer exceptions raised by category use cases.
 */
public final class CategoryApplicationExceptions {

    private CategoryApplicationExceptions() {
    }

    public static final class DuplicateCategorySlugException extends RuntimeException {

        public DuplicateCategorySlugException(String slug) {
            super("Category slug already exists: " + slug);
        }
    }

    public static final class CategoryNotFoundException extends RuntimeException {

        public CategoryNotFoundException(Long categoryId) {
            super("Category not found: " + categoryId);
        }
    }

    public static final class CategoryLocalizationAlreadyExistsException extends RuntimeException {

        public CategoryLocalizationAlreadyExistsException(Long categoryId, LanguageCode languageCode) {
            super("Category localization already exists for category " + categoryId + " and language " + languageCode);
        }
    }

    public static final class CategoryLocalizationNotFoundException extends RuntimeException {

        public CategoryLocalizationNotFoundException(Long categoryId, LanguageCode languageCode) {
            super("Category localization not found for category " + categoryId + " and language " + languageCode);
        }
    }

    public static final class CategoryLocalizationNotPublishedException extends RuntimeException {

        public CategoryLocalizationNotPublishedException(Long categoryId, LanguageCode languageCode) {
            super("Category localization is not published for category " + categoryId + " and language " + languageCode);
        }
    }

    public static final class CategoryContentNotFoundException extends RuntimeException {

        public CategoryContentNotFoundException(Long categoryId, LanguageCode languageCode, Long contentId) {
            super("Curated content not found for category " + categoryId + ", language "
                    + languageCode + " and content " + contentId);
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

    public static final class ContentReferenceNotFoundException extends RuntimeException {

        public ContentReferenceNotFoundException(Long contentId) {
            super("Content not found for category curation: " + contentId);
        }
    }

    public static final class ContentInactiveException extends RuntimeException {

        public ContentInactiveException(Long contentId) {
            super("Content is inactive for category curation: " + contentId);
        }
    }

    public static final class ContentLocalizationNotPublishedException extends RuntimeException {

        public ContentLocalizationNotPublishedException(Long contentId, LanguageCode languageCode) {
            super("Content localization is not published for content " + contentId + " and language " + languageCode);
        }
    }
}
