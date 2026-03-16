package com.tellpal.v2.category.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);

    List<Category> findAllByIsActiveTrue();

    List<Category> findAllByIsActiveTrueAndType(CategoryType type);
}
