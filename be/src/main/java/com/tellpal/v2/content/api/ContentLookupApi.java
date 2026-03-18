package com.tellpal.v2.content.api;

import java.util.Optional;

/**
 * Module-facing lookup API for content identity and stable metadata.
 */
public interface ContentLookupApi {

    /**
     * Finds content by persistent identifier.
     */
    Optional<ContentReference> findById(Long contentId);

    /**
     * Finds content by external key.
     */
    Optional<ContentReference> findByExternalKey(String externalKey);
}
