package com.tellpal.v2.category.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tellpal.v2.category.application.CategoryApplicationExceptions.CategoryContentNotFoundException;
import com.tellpal.v2.category.application.CategoryApplicationExceptions.CategoryLocalizationNotFoundException;
import com.tellpal.v2.category.application.CategoryApplicationExceptions.CategoryLocalizationNotPublishedException;
import com.tellpal.v2.category.application.CategoryApplicationExceptions.CategoryNotFoundException;
import com.tellpal.v2.category.application.CategoryManagementCommands.AddCategoryContentCommand;
import com.tellpal.v2.category.application.CategoryManagementCommands.RemoveCategoryContentCommand;
import com.tellpal.v2.category.application.CategoryManagementCommands.UpdateCategoryContentOrderCommand;
import com.tellpal.v2.category.application.CategoryManagementResults.CategoryContentRecord;
import com.tellpal.v2.category.domain.Category;
import com.tellpal.v2.category.domain.CategoryContent;
import com.tellpal.v2.category.domain.CategoryLocalization;
import com.tellpal.v2.category.domain.CategoryRepository;
import com.tellpal.v2.shared.domain.LanguageCode;

@Service
public class CategoryCurationService {

    private final CategoryRepository categoryRepository;
    private final CategoryContentReferenceValidator contentReferenceValidator;

    public CategoryCurationService(
            CategoryRepository categoryRepository,
            CategoryContentReferenceValidator contentReferenceValidator) {
        this.categoryRepository = categoryRepository;
        this.contentReferenceValidator = contentReferenceValidator;
    }

    @Transactional
    public CategoryContentRecord addContent(AddCategoryContentCommand command) {
        Category category = loadCategory(command.categoryId());
        requirePublishedLocalization(category, command.languageCode());
        contentReferenceValidator.requireCuratableContent(command.contentId(), command.languageCode());
        CategoryContent categoryContent = category.addContent(
                command.languageCode(),
                command.contentId(),
                command.displayOrder());
        return CategoryManagementMapper.toCategoryContentRecord(
                command.categoryId(),
                categoryRepository.save(category).findCuratedContent(command.languageCode(), command.contentId())
                        .orElse(categoryContent));
    }

    @Transactional
    public CategoryContentRecord updateContentOrder(UpdateCategoryContentOrderCommand command) {
        Category category = loadCategory(command.categoryId());
        requirePublishedLocalization(category, command.languageCode());
        loadCuratedContent(category, command.languageCode(), command.contentId());
        contentReferenceValidator.requireCuratableContent(command.contentId(), command.languageCode());
        CategoryContent categoryContent = category.updateContentOrder(
                command.languageCode(),
                command.contentId(),
                command.displayOrder());
        return CategoryManagementMapper.toCategoryContentRecord(
                command.categoryId(),
                categoryRepository.save(category).findCuratedContent(command.languageCode(), command.contentId())
                        .orElse(categoryContent));
    }

    @Transactional
    public void removeContent(RemoveCategoryContentCommand command) {
        Category category = loadCategory(command.categoryId());
        loadCuratedContent(category, command.languageCode(), command.contentId());
        category.removeContent(command.languageCode(), command.contentId());
        categoryRepository.save(category);
    }

    private Category loadCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
    }

    private CategoryLocalization requirePublishedLocalization(Category category, LanguageCode languageCode) {
        Long categoryId = requireCategoryId(category);
        CategoryLocalization localization = category.findLocalization(languageCode)
                .orElseThrow(() -> new CategoryLocalizationNotFoundException(categoryId, languageCode));
        if (!localization.isPublished()) {
            throw new CategoryLocalizationNotPublishedException(categoryId, languageCode);
        }
        return localization;
    }

    private CategoryContent loadCuratedContent(Category category, LanguageCode languageCode, Long contentId) {
        Long categoryId = requireCategoryId(category);
        return category.findCuratedContent(languageCode, contentId)
                .orElseThrow(() -> new CategoryContentNotFoundException(categoryId, languageCode, contentId));
    }

    private static Long requireCategoryId(Category category) {
        Long categoryId = category.getId();
        if (categoryId == null || categoryId <= 0) {
            throw new IllegalStateException("Category must be persisted before curation mapping");
        }
        return categoryId;
    }
}
