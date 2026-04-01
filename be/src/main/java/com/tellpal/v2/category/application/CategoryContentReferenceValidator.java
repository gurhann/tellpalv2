package com.tellpal.v2.category.application;

import org.springframework.stereotype.Component;

import com.tellpal.v2.category.application.CategoryApplicationExceptions.CategoryContentTypeMismatchException;
import com.tellpal.v2.category.application.CategoryApplicationExceptions.ContentInactiveException;
import com.tellpal.v2.category.application.CategoryApplicationExceptions.ContentLocalizationNotPublishedException;
import com.tellpal.v2.category.application.CategoryApplicationExceptions.ContentReferenceNotFoundException;
import com.tellpal.v2.category.domain.CategoryType;
import com.tellpal.v2.content.api.ContentReference;
import com.tellpal.v2.content.api.ContentLocalizationLookupApi;
import com.tellpal.v2.content.api.ContentLookupApi;
import com.tellpal.v2.shared.domain.LanguageCode;

@Component
final class CategoryContentReferenceValidator {

    private final ContentLookupApi contentLookupApi;
    private final ContentLocalizationLookupApi contentLocalizationLookupApi;

    CategoryContentReferenceValidator(
            ContentLookupApi contentLookupApi,
            ContentLocalizationLookupApi contentLocalizationLookupApi) {
        this.contentLookupApi = contentLookupApi;
        this.contentLocalizationLookupApi = contentLocalizationLookupApi;
    }

    void requireCuratableContent(CategoryType categoryType, Long contentId, LanguageCode languageCode) {
        CategoryType requiredCategoryType = requireCategoryType(categoryType);
        ContentReference content = contentLookupApi.findById(contentId)
                .orElseThrow(() -> new ContentReferenceNotFoundException(contentId));

        if (!content.active()) {
            throw new ContentInactiveException(contentId);
        }

        if (content.type() != requiredCategoryType.toContentApiType()) {
            throw new CategoryContentTypeMismatchException(contentId, requiredCategoryType, content.type());
        }

        boolean publishedLocalization = contentLocalizationLookupApi.findLocalization(contentId, languageCode)
                .map(localization -> localization.published())
                .orElse(false);
        if (!publishedLocalization) {
            throw new ContentLocalizationNotPublishedException(contentId, languageCode);
        }
    }

    private static CategoryType requireCategoryType(CategoryType categoryType) {
        if (categoryType == null) {
            throw new IllegalArgumentException("Category type must not be null");
        }
        return categoryType;
    }
}
