package com.tellpal.v2.category.application;

import com.tellpal.v2.category.domain.CategoryContent;
import com.tellpal.v2.category.domain.CategoryContentRepository;
import com.tellpal.v2.category.domain.CategoryLocalizationRepository;
import com.tellpal.v2.category.domain.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CurationService {

    private final CategoryRepository categoryRepository;
    private final CategoryContentRepository categoryContentRepository;
    private final CategoryLocalizationRepository categoryLocalizationRepository;

    public CurationService(
            CategoryRepository categoryRepository,
            CategoryContentRepository categoryContentRepository,
            CategoryLocalizationRepository categoryLocalizationRepository) {
        this.categoryRepository = categoryRepository;
        this.categoryContentRepository = categoryContentRepository;
        this.categoryLocalizationRepository = categoryLocalizationRepository;
    }

    public CategoryContent addContent(Long categoryId, String languageCode, Long contentId, int displayOrder) {
        categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
        CategoryContent content = new CategoryContent(categoryId, languageCode, contentId, displayOrder);
        return categoryContentRepository.save(content);
    }

    public CategoryContent updateContentOrder(Long categoryId, String languageCode, Long contentId, int newDisplayOrder) {
        CategoryContent content = categoryContentRepository
                .findByCategoryIdAndLanguageCodeAndContentId(categoryId, languageCode, contentId)
                .orElseThrow(() -> new RuntimeException("Content not found in category"));
        content.setDisplayOrder(newDisplayOrder);
        return categoryContentRepository.save(content);
    }

    public void removeContent(Long categoryId, String languageCode, Long contentId) {
        categoryContentRepository.deleteByCategoryIdAndLanguageCodeAndContentId(categoryId, languageCode, contentId);
    }

    @Transactional(readOnly = true)
    public List<CategoryContent> getCategoryContents(Long categoryId, String languageCode) {
        return categoryContentRepository
                .findByCategoryIdAndLanguageCodeOrderByDisplayOrderAsc(categoryId, languageCode);
    }
}
