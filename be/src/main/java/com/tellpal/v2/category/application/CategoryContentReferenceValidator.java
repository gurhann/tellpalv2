package com.tellpal.v2.category.application;

import org.springframework.stereotype.Component;

import com.tellpal.v2.category.application.CategoryApplicationExceptions.ContentInactiveException;
import com.tellpal.v2.category.application.CategoryApplicationExceptions.ContentLocalizationNotPublishedException;
import com.tellpal.v2.category.application.CategoryApplicationExceptions.ContentReferenceNotFoundException;
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

    void requireCuratableContent(Long contentId, LanguageCode languageCode) {
        boolean contentActive = contentLookupApi.findById(contentId)
                .map(content -> {
                    if (!content.active()) {
                        throw new ContentInactiveException(contentId);
                    }
                    return true;
                })
                .orElseThrow(() -> new ContentReferenceNotFoundException(contentId));

        if (!contentActive) {
            throw new IllegalStateException("Unexpected inactive content validation state");
        }

        boolean publishedLocalization = contentLocalizationLookupApi.findLocalization(contentId, languageCode)
                .map(localization -> localization.published())
                .orElse(false);
        if (!publishedLocalization) {
            throw new ContentLocalizationNotPublishedException(contentId, languageCode);
        }
    }
}
