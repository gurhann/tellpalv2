package com.tellpal.v2.category.application;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tellpal.v2.category.api.AdminCategoryContentView;
import com.tellpal.v2.category.api.AdminCategoryCurationQueryApi;
import com.tellpal.v2.category.application.CategoryApplicationExceptions.CategoryLocalizationNotFoundException;
import com.tellpal.v2.category.application.CategoryApplicationExceptions.CategoryNotFoundException;
import com.tellpal.v2.category.domain.Category;
import com.tellpal.v2.category.domain.CategoryRepository;
import com.tellpal.v2.shared.domain.LanguageCode;

/**
 * Read-only application service for localized category curation collections.
 *
 * <p>The service returns the stored content links for one category-language lane without requiring
 * the localization to be published.
 */
@Service
@Transactional(readOnly = true)
public class AdminCategoryCurationQueryService implements AdminCategoryCurationQueryApi {

    private final CategoryRepository categoryRepository;

    public AdminCategoryCurationQueryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /**
     * Returns the ordered curated content collection for one existing category localization.
     */
    @Override
    public List<AdminCategoryContentView> listCategoryContents(Long categoryId, LanguageCode languageCode) {
        LanguageCode requiredLanguageCode = requireLanguageCode(languageCode);
        Category category = categoryRepository.findById(requireCategoryId(categoryId))
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
        Long requiredCategoryId = requirePersistedCategoryId(category);
        category.findLocalization(requiredLanguageCode)
                .orElseThrow(() -> new CategoryLocalizationNotFoundException(requiredCategoryId, requiredLanguageCode));
        return category.getCuratedContents().stream()
                .filter(categoryContent -> categoryContent.matchesLanguage(requiredLanguageCode))
                .sorted(Comparator.comparingInt(com.tellpal.v2.category.domain.CategoryContent::getDisplayOrder)
                        .thenComparing(com.tellpal.v2.category.domain.CategoryContent::getContentId))
                .map(categoryContent -> new AdminCategoryContentView(
                        requiredCategoryId,
                        categoryContent.getLanguageCode(),
                        categoryContent.getContentId(),
                        categoryContent.getDisplayOrder()))
                .toList();
    }

    private static Long requireCategoryId(Long categoryId) {
        if (categoryId == null || categoryId <= 0) {
            throw new IllegalArgumentException("Category ID must be positive");
        }
        return categoryId;
    }

    private static LanguageCode requireLanguageCode(LanguageCode languageCode) {
        if (languageCode == null) {
            throw new IllegalArgumentException("Language code must not be null");
        }
        return languageCode;
    }

    private static Long requirePersistedCategoryId(Category category) {
        Long categoryId = category.getId();
        if (categoryId == null || categoryId <= 0) {
            throw new IllegalStateException("Category must be persisted before curation query mapping");
        }
        return categoryId;
    }
}
