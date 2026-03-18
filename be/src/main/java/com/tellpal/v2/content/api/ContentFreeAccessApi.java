package com.tellpal.v2.content.api;

import com.tellpal.v2.shared.domain.LanguageCode;

/**
 * Module-facing use cases for resolving localized free-access rules for content.
 */
public interface ContentFreeAccessApi {

    /**
     * Resolves the effective free-access set for a language and requested access key.
     *
     * <p>Implementations may fall back to the default access key when the requested one is unknown.
     */
    ResolvedContentFreeAccessSet resolveFreeAccess(LanguageCode languageCode, String requestedAccessKey);

    /**
     * Checks whether a single content item is free under the effective localized access set.
     */
    boolean isContentFree(Long contentId, LanguageCode languageCode, String requestedAccessKey);
}
