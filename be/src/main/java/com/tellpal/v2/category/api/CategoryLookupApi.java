package com.tellpal.v2.category.api;

import java.util.Optional;

/**
 * Module-facing lookup API for category identity and stable metadata.
 */
public interface CategoryLookupApi {

    /**
     * Finds a category by ID.
     */
    Optional<CategoryReference> findById(Long categoryId);

    /**
     * Finds a category by slug.
     */
    Optional<CategoryReference> findBySlug(String slug);
}
