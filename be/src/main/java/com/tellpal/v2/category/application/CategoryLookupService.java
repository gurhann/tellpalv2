package com.tellpal.v2.category.application;

import java.util.List;
import java.util.Optional;
import java.util.Comparator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tellpal.v2.category.api.AdminCategoryLocalizationView;
import com.tellpal.v2.category.api.CategoryLookupApi;
import com.tellpal.v2.category.api.CategoryReference;
import com.tellpal.v2.category.application.CategoryApplicationExceptions.CategoryNotFoundException;
import com.tellpal.v2.category.domain.CategoryRepository;

/**
 * Read-only application service for category identity lookup.
 */
@Service
@Transactional(readOnly = true)
public class CategoryLookupService implements CategoryLookupApi {

    private final CategoryRepository categoryRepository;

    public CategoryLookupService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /**
     * Returns all categories for admin list screens, including inactive entries.
     */
    @Override
    public List<CategoryReference> listAll() {
        return categoryRepository.findAllForAdminRead().stream()
                .map(CategoryApiMapper::toReference)
                .toList();
    }

    /**
     * Returns all localizations for one category in a stable language order.
     */
    @Override
    public List<AdminCategoryLocalizationView> listLocalizations(Long categoryId) {
        Long requiredCategoryId = requireCategoryId(categoryId);
        return categoryRepository.findById(requiredCategoryId)
                .orElseThrow(() -> new CategoryNotFoundException(requiredCategoryId))
                .getLocalizations()
                .stream()
                .sorted(Comparator.comparing(localization -> localization.getLanguageCode().value()))
                .map(localization -> CategoryApiMapper.toLocalizationView(requiredCategoryId, localization))
                .toList();
    }

    /**
     * Finds a category by ID and maps it to the module-facing reference type.
     */
    @Override
    public Optional<CategoryReference> findById(Long categoryId) {
        return categoryRepository.findById(requireCategoryId(categoryId))
                .map(CategoryApiMapper::toReference);
    }

    /**
     * Finds a category by slug and maps it to the module-facing reference type.
     */
    @Override
    public Optional<CategoryReference> findBySlug(String slug) {
        return categoryRepository.findBySlug(requireSlug(slug))
                .map(CategoryApiMapper::toReference);
    }

    private static Long requireCategoryId(Long categoryId) {
        if (categoryId == null || categoryId <= 0) {
            throw new IllegalArgumentException("Category ID must be positive");
        }
        return categoryId;
    }

    private static String requireSlug(String slug) {
        if (slug == null || slug.isBlank()) {
            throw new IllegalArgumentException("Category slug must not be blank");
        }
        return slug.trim();
    }
}
