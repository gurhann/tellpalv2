package com.tellpal.v2.content.application;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tellpal.v2.content.api.AdminContentQueryApi;
import com.tellpal.v2.content.api.AdminContentView;
import com.tellpal.v2.content.api.ContentApiType;
import com.tellpal.v2.content.api.EligibleContentQueryApi;
import com.tellpal.v2.content.api.EligibleContentView;
import com.tellpal.v2.content.domain.Content;
import com.tellpal.v2.content.domain.ContentLocalization;
import com.tellpal.v2.content.domain.LocalizationStatus;
import com.tellpal.v2.content.domain.ContentRepository;
import com.tellpal.v2.shared.domain.LanguageCode;

/**
 * Read-only application service for admin content queries.
 *
 * <p>The service returns metadata plus localized content snapshots needed by CMS list and detail
 * screens, including inactive content items.
 */
@Service
@Transactional(readOnly = true)
public class AdminContentQueryService implements AdminContentQueryApi, EligibleContentQueryApi {

    private final ContentRepository contentRepository;

    public AdminContentQueryService(ContentRepository contentRepository) {
        this.contentRepository = contentRepository;
    }

    /**
     * Returns all content aggregates for CMS operators with their localization snapshots.
     */
    @Override
    public List<AdminContentView> listContents() {
        return contentRepository.findAllForAdminRead().stream()
                .map(ContentAdminQueryMapper::toView)
                .toList();
    }

    /**
     * Returns one content aggregate and its localization snapshots when it exists.
     */
    @Override
    public Optional<AdminContentView> findContent(Long contentId) {
        return contentRepository.findByIdForAdminRead(requireContentId(contentId))
                .map(ContentAdminQueryMapper::toView);
    }

    /**
     * Returns active content candidates whose requested localization is published.
     */
    @Override
    public List<EligibleContentView> listEligibleContent(
            ContentApiType contentType,
            LanguageCode languageCode,
            String query,
            int limit) {
        ContentApiType requiredContentType = requireContentType(contentType);
        LanguageCode requiredLanguageCode = requireLanguageCode(languageCode);
        int requiredLimit = requireLimit(limit);
        Predicate<EligibleContentView> queryFilter = createQueryFilter(query);
        return contentRepository.findAllForAdminRead().stream()
                .filter(Content::isActive)
                .filter(content -> ContentApiType.valueOf(content.getType().name()) == requiredContentType)
                .flatMap(content -> content.findLocalization(requiredLanguageCode)
                        .filter(localization -> localization.getStatus() == LocalizationStatus.PUBLISHED)
                        .stream()
                        .map(localization -> new EligibleContentView(
                                requireContentId(content.getId()),
                                content.getExternalKey(),
                                localization.getTitle(),
                                localization.getLanguageCode(),
                                localization.getPublishedAt())))
                .filter(queryFilter)
                .sorted(java.util.Comparator.comparing(EligibleContentView::localizedTitle)
                        .thenComparing(EligibleContentView::externalKey)
                        .thenComparing(EligibleContentView::contentId))
                .limit(requiredLimit)
                .toList();
    }

    private static Long requireContentId(Long contentId) {
        if (contentId == null || contentId <= 0) {
            throw new IllegalArgumentException("Content ID must be positive");
        }
        return contentId;
    }

    private static ContentApiType requireContentType(ContentApiType contentType) {
        if (contentType == null) {
            throw new IllegalArgumentException("Content type must not be null");
        }
        return contentType;
    }

    private static LanguageCode requireLanguageCode(LanguageCode languageCode) {
        if (languageCode == null) {
            throw new IllegalArgumentException("Language code must not be null");
        }
        return languageCode;
    }

    private static int requireLimit(int limit) {
        if (limit < 1 || limit > 50) {
            throw new IllegalArgumentException("Limit must be between 1 and 50");
        }
        return limit;
    }

    private static Predicate<EligibleContentView> createQueryFilter(String query) {
        if (query == null || query.isBlank()) {
            return candidate -> true;
        }
        String normalizedQuery = query.trim().toLowerCase();
        return candidate -> candidate.localizedTitle().toLowerCase().contains(normalizedQuery)
                || candidate.externalKey().toLowerCase().contains(normalizedQuery)
                || candidate.contentId().toString().contains(normalizedQuery);
    }
}
