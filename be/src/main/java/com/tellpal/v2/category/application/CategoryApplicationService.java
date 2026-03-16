package com.tellpal.v2.category.application;

import com.tellpal.v2.category.domain.Category;
import com.tellpal.v2.category.domain.CategoryLocalization;
import com.tellpal.v2.category.domain.CategoryLocalizationRepository;
import com.tellpal.v2.category.domain.CategoryRepository;
import com.tellpal.v2.category.domain.CategoryType;
import com.tellpal.v2.shared.domain.LocalizationStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CategoryApplicationService {

    private final CategoryRepository categoryRepository;
    private final CategoryLocalizationRepository categoryLocalizationRepository;

    public CategoryApplicationService(
            CategoryRepository categoryRepository,
            CategoryLocalizationRepository categoryLocalizationRepository) {
        this.categoryRepository = categoryRepository;
        this.categoryLocalizationRepository = categoryLocalizationRepository;
    }

    public Category createCategory(String slug, CategoryType type, boolean isPremium) {
        Category category = new Category(slug, type);
        category.setPremium(isPremium);
        return categoryRepository.save(category);
    }

    public Category updateCategory(Long id, Boolean isActive, Boolean isPremium) {
        Category category = getCategory(id);
        if (isActive != null) {
            category.setActive(isActive);
        }
        if (isPremium != null) {
            category.setPremium(isPremium);
        }
        return categoryRepository.save(category);
    }

    @Transactional(readOnly = true)
    public Category getCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Category getCategoryBySlug(String slug) {
        return categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new CategoryNotFoundException(-1L));
    }

    @Transactional(readOnly = true)
    public List<Category> listCategories(CategoryType type) {
        if (type != null) {
            return categoryRepository.findAllByIsActiveTrueAndType(type);
        }
        return categoryRepository.findAllByIsActiveTrue();
    }

    public CategoryLocalization createLocalization(Long categoryId, String languageCode,
            String name, String description) {
        Category category = getCategory(categoryId);
        CategoryLocalization localization = new CategoryLocalization(category, languageCode, name);
        localization.setDescription(description);
        return categoryLocalizationRepository.save(localization);
    }

    public CategoryLocalization updateLocalization(Long categoryId, String languageCode,
            String name, String description) {
        CategoryLocalization localization = categoryLocalizationRepository
                .findByCategoryIdAndLanguageCode(categoryId, languageCode)
                .orElseThrow(() -> new CategoryLocalizationNotFoundException(categoryId, languageCode));
        if (name != null) {
            localization.setName(name);
        }
        if (description != null) {
            localization.setDescription(description);
        }
        return categoryLocalizationRepository.save(localization);
    }

    @Transactional(readOnly = true)
    public Optional<CategoryLocalization> findLocalization(Long categoryId, String languageCode) {
        return categoryLocalizationRepository.findByCategoryIdAndLanguageCode(categoryId, languageCode);
    }

    public CategoryLocalization publishLocalization(Long categoryId, String languageCode) {
        CategoryLocalization localization = categoryLocalizationRepository
                .findByCategoryIdAndLanguageCode(categoryId, languageCode)
                .orElseThrow(() -> new CategoryLocalizationNotFoundException(categoryId, languageCode));
        localization.setStatus(LocalizationStatus.PUBLISHED);
        localization.setPublishedAt(OffsetDateTime.now());
        return categoryLocalizationRepository.save(localization);
    }
}
