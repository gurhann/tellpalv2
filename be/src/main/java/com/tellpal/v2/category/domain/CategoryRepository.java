package com.tellpal.v2.category.domain;

import java.util.Optional;

public interface CategoryRepository {

    Optional<Category> findById(Long id);

    Optional<Category> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Category save(Category category);
}
