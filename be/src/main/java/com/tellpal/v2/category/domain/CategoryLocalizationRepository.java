package com.tellpal.v2.category.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryLocalizationRepository extends JpaRepository<CategoryLocalization, CategoryLocalizationId> {

    Optional<CategoryLocalization> findByCategoryIdAndLanguageCode(Long categoryId, String languageCode);

    List<CategoryLocalization> findByCategoryId(Long categoryId);
}
