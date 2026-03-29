package com.tellpal.v2.content.api;

import java.util.List;
import java.util.Optional;

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
}
