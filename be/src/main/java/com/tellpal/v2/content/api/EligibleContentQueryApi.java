package com.tellpal.v2.content.api;

import java.util.List;

import com.tellpal.v2.shared.domain.LanguageCode;

/**
 * Read API for lightweight content candidates used by admin pickers.
 */
public interface EligibleContentQueryApi {

    /**
     * Returns active published content candidates for one type and language.
     */
    List<EligibleContentView> listEligibleContent(
            ContentApiType contentType,
            LanguageCode languageCode,
            String query,
            int limit);
}
