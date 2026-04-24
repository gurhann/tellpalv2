package com.tellpal.v2.content.api;

import java.util.Collection;
import java.util.Map;

import com.tellpal.v2.shared.domain.LanguageCode;

/**
 * Lookup API for localized content identity used by other modules.
 */
public interface LocalizedContentIdentityLookupApi {

    /**
     * Resolves localized identity details for the provided content ids in one language.
     */
    Map<Long, LocalizedContentIdentityReference> findLocalizedIdentities(
            Collection<Long> contentIds,
            LanguageCode languageCode);
}
