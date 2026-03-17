package com.tellpal.v2.category.api;

import java.util.Optional;

public interface CategoryLookupApi {

    Optional<CategoryReference> findById(Long categoryId);

    Optional<CategoryReference> findBySlug(String slug);
}
