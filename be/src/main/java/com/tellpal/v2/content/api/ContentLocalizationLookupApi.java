package com.tellpal.v2.content.api;

import java.util.Optional;

import com.tellpal.v2.shared.domain.LanguageCode;

/**
 * Lookup API for localized content visibility state.
 */
public interface ContentLocalizationLookupApi {

    /**
     * Returns localization visibility data when the content item exists for the requested language.
     */
    Optional<ContentLocalizationReference> findLocalization(Long contentId, LanguageCode languageCode);
}
