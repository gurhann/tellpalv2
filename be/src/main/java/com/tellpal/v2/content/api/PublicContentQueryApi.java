package com.tellpal.v2.content.api;

import java.util.List;
import java.util.Optional;

import com.tellpal.v2.shared.domain.LanguageCode;

/**
 * Read-model API for mobile-facing content discovery and detail views.
 */
public interface PublicContentQueryApi {

    /**
     * Lists visible content summaries for a language, optionally filtered by content type.
     */
    List<PublicContentSummary> listContents(LanguageCode languageCode, String requestedAccessKey, ContentApiType type);

    /**
     * Lists visible content summaries for a fixed set of content identifiers.
     */
    List<PublicContentSummary> listContentsByIds(
            List<Long> contentIds,
            LanguageCode languageCode,
            String requestedAccessKey);

    /**
     * Returns public details for a visible content localization.
     */
    Optional<PublicContentDetails> findContent(Long contentId, LanguageCode languageCode, String requestedAccessKey);

    /**
     * Returns localized story pages when the content is a visible story in the requested language.
     */
    Optional<List<PublicStoryPage>> findStoryPages(Long contentId, LanguageCode languageCode);
}
