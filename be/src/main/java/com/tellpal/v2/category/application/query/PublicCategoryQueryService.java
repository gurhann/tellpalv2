package com.tellpal.v2.category.application.query;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tellpal.v2.asset.api.AssetRecord;
import com.tellpal.v2.asset.api.AssetRegistryApi;
import com.tellpal.v2.category.api.CategoryApiType;
import com.tellpal.v2.category.api.PublicCategoryQueryApi;
import com.tellpal.v2.category.api.PublicCategoryView;
import com.tellpal.v2.category.domain.Category;
import com.tellpal.v2.category.domain.CategoryContent;
import com.tellpal.v2.category.domain.CategoryLocalization;
import com.tellpal.v2.category.domain.CategoryRepository;
import com.tellpal.v2.content.api.PublicContentQueryApi;
import com.tellpal.v2.content.api.PublicContentSummary;
import com.tellpal.v2.shared.domain.LanguageCode;

/**
 * Read-model service for mobile-facing category queries.
 *
 * <p>The service exposes only active categories with published localizations and resolves curated
 * content through the public content query API.
 */
@Service
@Transactional(readOnly = true)
public class PublicCategoryQueryService implements PublicCategoryQueryApi {

    private final CategoryRepository categoryRepository;
    private final PublicContentQueryApi publicContentQueryApi;
    private final AssetRegistryApi assetRegistryApi;

    public PublicCategoryQueryService(
            CategoryRepository categoryRepository,
            PublicContentQueryApi publicContentQueryApi,
            AssetRegistryApi assetRegistryApi) {
        this.categoryRepository = categoryRepository;
        this.publicContentQueryApi = publicContentQueryApi;
        this.assetRegistryApi = assetRegistryApi;
    }

    @Override
    /**
     * Lists visible localized categories for one language and optional type filter.
     */
    public List<PublicCategoryView> listCategories(LanguageCode languageCode, CategoryApiType type) {
        LanguageCode requiredLanguageCode = requireLanguageCode(languageCode);
        return categoryRepository.findAllActive().stream()
                .filter(category -> type == null || CategoryApiType.valueOf(category.getType().name()) == type)
                .sorted(java.util.Comparator.comparing(Category::getSlug))
                .flatMap(category -> publishedLocalization(category, requiredLanguageCode)
                        .stream()
                        .map(localization -> toView(category, localization)))
                .toList();
    }

    @Override
    /**
     * Finds one visible localized category by slug.
     */
    public Optional<PublicCategoryView> findCategory(String slug, LanguageCode languageCode) {
        LanguageCode requiredLanguageCode = requireLanguageCode(languageCode);
        return categoryRepository.findBySlug(requireSlug(slug))
                .filter(Category::isActive)
                .flatMap(category -> publishedLocalization(category, requiredLanguageCode)
                        .map(localization -> toView(category, localization)));
    }

    @Override
    /**
     * Lists curated content summaries for a visible localized category.
     */
    public List<PublicContentSummary> listCategoryContents(String slug, LanguageCode languageCode, String requestedAccessKey) {
        LanguageCode requiredLanguageCode = requireLanguageCode(languageCode);
        return categoryRepository.findBySlug(requireSlug(slug))
                .filter(Category::isActive)
                .flatMap(category -> publishedLocalization(category, requiredLanguageCode)
                        .map(localization -> orderedContentIds(category, requiredLanguageCode)))
                .map(contentIds -> publicContentQueryApi.listContentsByIds(contentIds, requiredLanguageCode, requestedAccessKey))
                .orElse(List.of());
    }

    private PublicCategoryView toView(Category category, CategoryLocalization localization) {
        return PublicCategoryQueryMapper.toView(category, localization, loadAsset(localization.getImageMediaId()));
    }

    private List<Long> orderedContentIds(Category category, LanguageCode languageCode) {
        return category.getCuratedContents().stream()
                .filter(candidate -> candidate.matchesLanguage(languageCode))
                .sorted(java.util.Comparator.comparingInt(CategoryContent::getDisplayOrder))
                .map(CategoryContent::getContentId)
                .toList();
    }

    private Optional<CategoryLocalization> publishedLocalization(Category category, LanguageCode languageCode) {
        return category.findLocalization(languageCode)
                .filter(CategoryLocalization::isPublished);
    }

    private AssetRecord loadAsset(Long assetId) {
        if (assetId == null) {
            return null;
        }
        return assetRegistryApi.findById(assetId).orElse(null);
    }

    private static String requireSlug(String slug) {
        if (slug == null || slug.isBlank()) {
            throw new IllegalArgumentException("Category slug must not be blank");
        }
        return slug.trim();
    }

    private static LanguageCode requireLanguageCode(LanguageCode languageCode) {
        if (languageCode == null) {
            throw new IllegalArgumentException("Language code must not be null");
        }
        return languageCode;
    }
}
