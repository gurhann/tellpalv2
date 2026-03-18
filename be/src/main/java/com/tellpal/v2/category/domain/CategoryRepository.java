package com.tellpal.v2.category.domain;

import java.util.List;
import java.util.Optional;

/**
 * Loads and persists curated {@link Category} aggregates.
 */
public interface CategoryRepository {

    /**
     * Returns the category aggregate for the internal id when it exists.
     */
    Optional<Category> findById(Long id);

    /**
     * Resolves a category by the public slug used in read APIs and linking flows.
     */
    Optional<Category> findBySlug(String slug);

    /**
     * Checks whether the slug is already reserved by another category.
     */
    boolean existsBySlug(String slug);

    /**
     * Returns active categories that can still appear in public discovery surfaces.
     */
    List<Category> findAllActive();

    /**
     * Persists category metadata, localization changes and curated content links.
     */
    Category save(Category category);
}
