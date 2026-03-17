package com.tellpal.v2.content.api;

import java.util.Optional;

import com.tellpal.v2.shared.domain.LanguageCode;

public interface ContentLocalizationLookupApi {

    Optional<ContentLocalizationReference> findLocalization(Long contentId, LanguageCode languageCode);
}
