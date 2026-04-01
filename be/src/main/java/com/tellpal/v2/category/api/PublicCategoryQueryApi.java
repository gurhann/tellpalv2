package com.tellpal.v2.category.api;

import java.util.List;
import java.util.Optional;

import com.tellpal.v2.content.api.ContentApiType;
import com.tellpal.v2.content.api.PublicContentSummary;
import com.tellpal.v2.shared.domain.LanguageCode;

/**
 * Read-model API for mobile-facing category discovery and localized content curation.
 */
public interface PublicCategoryQueryApi {

    /**
     * Lists visible categories for a language and optional category type filter.
     */
    List<PublicCategoryView> listCategories(LanguageCode languageCode, ContentApiType type);

    /**
     * Finds one visible localized category by slug.
     */
    Optional<PublicCategoryView> findCategory(String slug, LanguageCode languageCode);

    /**
     * Lists the curated content summaries for a visible localized category.
     */
    List<PublicContentSummary> listCategoryContents(String slug, LanguageCode languageCode, String requestedAccessKey);
}
