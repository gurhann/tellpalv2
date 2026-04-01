package com.tellpal.v2.category.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tellpal.v2.category.api.CategoryReference;
import com.tellpal.v2.category.application.CategoryApplicationExceptions.CategoryLocalizationAlreadyExistsException;
import com.tellpal.v2.category.application.CategoryApplicationExceptions.CategoryLocalizationNotFoundException;
import com.tellpal.v2.category.application.CategoryApplicationExceptions.CategoryNotFoundException;
import com.tellpal.v2.category.application.CategoryApplicationExceptions.DuplicateCategorySlugException;
import com.tellpal.v2.category.application.CategoryManagementCommands.CreateCategoryCommand;
import com.tellpal.v2.category.application.CategoryManagementCommands.CreateCategoryLocalizationCommand;
import com.tellpal.v2.category.application.CategoryManagementCommands.DeleteCategoryCommand;
import com.tellpal.v2.category.application.CategoryManagementCommands.UpdateCategoryCommand;
import com.tellpal.v2.category.application.CategoryManagementCommands.UpdateCategoryLocalizationCommand;
import com.tellpal.v2.category.application.CategoryManagementResults.CategoryLocalizationRecord;
import com.tellpal.v2.category.domain.Category;
import com.tellpal.v2.category.domain.CategoryLocalization;
import com.tellpal.v2.category.domain.CategoryRepository;
import com.tellpal.v2.shared.domain.LanguageCode;

/**
 * Application service for creating and updating categories and their localizations.
 */
@Service
public class CategoryManagementService {

    private final CategoryRepository categoryRepository;
    private final CategoryAssetReferenceValidator assetReferenceValidator;

    public CategoryManagementService(
            CategoryRepository categoryRepository,
            CategoryAssetReferenceValidator assetReferenceValidator) {
        this.categoryRepository = categoryRepository;
        this.assetReferenceValidator = assetReferenceValidator;
    }

    /**
     * Creates a new category after verifying slug uniqueness.
     */
    @Transactional
    public CategoryReference createCategory(CreateCategoryCommand command) {
        ensureSlugAvailable(null, command.slug());
        Category savedCategory = categoryRepository.save(Category.create(
                command.slug(),
                command.type(),
                command.premium(),
                command.active()));
        return CategoryApiMapper.toReference(savedCategory);
    }

    /**
     * Updates core category metadata while preserving aggregate identity.
     */
    @Transactional
    public CategoryReference updateCategory(UpdateCategoryCommand command) {
        Category category = loadCategory(command.categoryId());
        ensureSlugAvailable(command.categoryId(), command.slug());
        category.updateDetails(command.slug(), command.type(), command.premium(), command.active());
        return CategoryApiMapper.toReference(categoryRepository.save(category));
    }

    /**
     * Deactivates a category aggregate while preserving localization and curation history.
     */
    @Transactional
    public void deleteCategory(DeleteCategoryCommand command) {
        Category category = loadCategory(command.categoryId());
        category.deactivate();
        categoryRepository.save(category);
    }

    /**
     * Creates a new localized category view after validating the image asset reference.
     */
    @Transactional
    public CategoryLocalizationRecord createLocalization(CreateCategoryLocalizationCommand command) {
        Category category = loadCategory(command.categoryId());
        if (category.findLocalization(command.languageCode()).isPresent()) {
            throw new CategoryLocalizationAlreadyExistsException(command.categoryId(), command.languageCode());
        }
        assetReferenceValidator.requireImageAsset(command.imageMediaId(), "imageMediaId");
        CategoryLocalization localization = category.upsertLocalization(
                command.languageCode(),
                command.name(),
                command.description(),
                command.imageMediaId(),
                command.status(),
                command.publishedAt());
        return CategoryManagementMapper.toLocalizationRecord(
                command.categoryId(),
                categoryRepository.save(category).findLocalization(command.languageCode())
                        .orElse(localization));
    }

    /**
     * Replaces localized category content for one language.
     */
    @Transactional
    public CategoryLocalizationRecord updateLocalization(UpdateCategoryLocalizationCommand command) {
        Category category = loadCategory(command.categoryId());
        loadLocalization(category, command.languageCode());
        assetReferenceValidator.requireImageAsset(command.imageMediaId(), "imageMediaId");
        CategoryLocalization localization = category.upsertLocalization(
                command.languageCode(),
                command.name(),
                command.description(),
                command.imageMediaId(),
                command.status(),
                command.publishedAt());
        return CategoryManagementMapper.toLocalizationRecord(
                command.categoryId(),
                categoryRepository.save(category).findLocalization(command.languageCode())
                        .orElse(localization));
    }

    private Category loadCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
    }

    private CategoryLocalization loadLocalization(Category category, LanguageCode languageCode) {
        Long categoryId = requireCategoryId(category);
        return category.findLocalization(languageCode)
                .orElseThrow(() -> new CategoryLocalizationNotFoundException(categoryId, languageCode));
    }

    private void ensureSlugAvailable(Long currentCategoryId, String slug) {
        categoryRepository.findBySlug(slug)
                .filter(candidate -> !requireCategoryId(candidate).equals(currentCategoryId))
                .ifPresent(candidate -> {
                    throw new DuplicateCategorySlugException(slug);
                });
    }

    private static Long requireCategoryId(Category category) {
        Long categoryId = category.getId();
        if (categoryId == null || categoryId <= 0) {
            throw new IllegalStateException("Category must be persisted before application mapping");
        }
        return categoryId;
    }
}
