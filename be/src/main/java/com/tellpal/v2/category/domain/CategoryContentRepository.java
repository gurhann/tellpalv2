package com.tellpal.v2.category.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface CategoryContentRepository extends JpaRepository<CategoryContent, Long> {

    List<CategoryContent> findByCategoryIdAndLanguageCodeOrderByDisplayOrderAsc(Long categoryId, String languageCode);

    Optional<CategoryContent> findByCategoryIdAndLanguageCodeAndContentId(Long categoryId, String languageCode, Long contentId);

    @Transactional
    void deleteByCategoryIdAndLanguageCodeAndContentId(Long categoryId, String languageCode, Long contentId);
}
