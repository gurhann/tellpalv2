package com.tellpal.v2.content.api;

import java.util.List;
import java.util.Optional;

import com.tellpal.v2.shared.domain.LanguageCode;

/**
 * Admin-facing read API for content metadata and localization snapshots.
 */
public interface AdminContentQueryApi {

    /**
     * Returns all content aggregates visible to CMS operators, including inactive entries.
     */
    List<AdminContentView> listContents();

    /**
     * Returns one content aggregate with its localized snapshots when it exists.
     */
    Optional<AdminContentView> findContent(Long contentId);

    /**
     * Returns language-specific, editor-ready registry rows with server-side filtering.
     */
    AdminContentRegistryPage listRegistry(
            LanguageCode languageCode,
            ContentApiType type,
            AdminContentRegistryReadiness readiness,
            String query,
            int page,
            int size);
}
