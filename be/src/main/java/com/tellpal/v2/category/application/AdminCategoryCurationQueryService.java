package com.tellpal.v2.category.application;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tellpal.v2.category.api.AdminCategoryContentView;
import com.tellpal.v2.category.api.AdminCategoryCurationQueryApi;
import com.tellpal.v2.category.api.AdminEligibleCategoryContentView;
import com.tellpal.v2.category.application.CategoryApplicationExceptions.CategoryLocalizationNotFoundException;
import com.tellpal.v2.category.application.CategoryApplicationExceptions.CategoryNotFoundException;
import com.tellpal.v2.category.domain.Category;
import com.tellpal.v2.category.domain.CategoryRepository;
import com.tellpal.v2.content.api.EligibleContentQueryApi;
import com.tellpal.v2.content.api.LocalizedContentIdentityLookupApi;
import com.tellpal.v2.content.api.LocalizedContentIdentityReference;
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
    private final EligibleContentQueryApi eligibleContentQueryApi;
    private final LocalizedContentIdentityLookupApi localizedContentIdentityLookupApi;

    public AdminCategoryCurationQueryService(
            CategoryRepository categoryRepository,
            EligibleContentQueryApi eligibleContentQueryApi,
            LocalizedContentIdentityLookupApi localizedContentIdentityLookupApi) {
        this.categoryRepository = categoryRepository;
        this.eligibleContentQueryApi = eligibleContentQueryApi;
        this.localizedContentIdentityLookupApi = localizedContentIdentityLookupApi;
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
        List<com.tellpal.v2.category.domain.CategoryContent> curatedContents = category.getCuratedContents().stream()
                .filter(categoryContent -> categoryContent.matchesLanguage(requiredLanguageCode))
                .sorted(Comparator.comparingInt(com.tellpal.v2.category.domain.CategoryContent::getDisplayOrder)
                        .thenComparing(com.tellpal.v2.category.domain.CategoryContent::getContentId))
                .toList();
        Map<Long, LocalizedContentIdentityReference> localizedIdentities = localizedContentIdentityLookupApi
                .findLocalizedIdentities(
                        curatedContents.stream()
                                .map(com.tellpal.v2.category.domain.CategoryContent::getContentId)
                                .toList(),
                        requiredLanguageCode);
        return curatedContents.stream()
                .map(categoryContent -> {
                    LocalizedContentIdentityReference identity = localizedIdentities.get(categoryContent.getContentId());
                    return new AdminCategoryContentView(
                            requiredCategoryId,
                            categoryContent.getLanguageCode(),
                            categoryContent.getContentId(),
                            categoryContent.getDisplayOrder(),
                            identity != null
                                    ? identity.externalKey()
                                    : fallbackExternalKey(categoryContent.getContentId()),
                            identity != null ? identity.localizedTitle() : null);
                })
                .toList();
    }

    /**
     * Returns content candidates that can be added to one existing category localization lane.
     */
    @Override
    public List<AdminEligibleCategoryContentView> listEligibleCategoryContents(
            Long categoryId,
            LanguageCode languageCode,
            String query,
            int limit) {
        LanguageCode requiredLanguageCode = requireLanguageCode(languageCode);
        Category category = categoryRepository.findById(requireCategoryId(categoryId))
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
        Long requiredCategoryId = requirePersistedCategoryId(category);
        category.findLocalization(requiredLanguageCode)
                .orElseThrow(() -> new CategoryLocalizationNotFoundException(requiredCategoryId, requiredLanguageCode));
        Set<Long> curatedContentIds = category.getCuratedContents().stream()
                .filter(categoryContent -> categoryContent.matchesLanguage(requiredLanguageCode))
                .map(com.tellpal.v2.category.domain.CategoryContent::getContentId)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        return eligibleContentQueryApi.listEligibleContent(
                        category.getType().toContentApiType(),
                        requiredLanguageCode,
                        query,
                        limit)
                .stream()
                .filter(candidate -> !curatedContentIds.contains(candidate.contentId()))
                .map(candidate -> new AdminEligibleCategoryContentView(
                        candidate.contentId(),
                        candidate.externalKey(),
                        candidate.localizedTitle(),
                        candidate.languageCode(),
                        candidate.publishedAt()))
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

    private static String fallbackExternalKey(Long contentId) {
        return "content-" + contentId;
    }
}
