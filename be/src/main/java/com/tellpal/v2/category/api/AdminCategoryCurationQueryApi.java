package com.tellpal.v2.category.api;

import java.util.List;

import com.tellpal.v2.shared.domain.LanguageCode;

/**
 * Admin-facing read API for one localized category curation lane.
 */
public interface AdminCategoryCurationQueryApi {

    /**
     * Returns the curated content collection for one category localization.
     */
    List<AdminCategoryContentView> listCategoryContents(Long categoryId, LanguageCode languageCode);
}
