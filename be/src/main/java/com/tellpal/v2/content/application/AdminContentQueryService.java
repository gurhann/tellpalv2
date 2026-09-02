package com.tellpal.v2.content.application;

import java.util.List;
import java.util.Optional;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.function.Predicate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tellpal.v2.content.api.AdminContentQueryApi;
import com.tellpal.v2.content.api.AdminContentView;
import com.tellpal.v2.content.api.AdminContentRegistryBlocker;
import com.tellpal.v2.content.api.AdminContentRegistryItem;
import com.tellpal.v2.content.api.AdminContentRegistryPage;
import com.tellpal.v2.content.api.AdminContentRegistryReadiness;
import com.tellpal.v2.content.api.ContentApiType;
import com.tellpal.v2.content.api.EligibleContentQueryApi;
import com.tellpal.v2.content.api.EligibleContentView;
import com.tellpal.v2.content.domain.Content;
import com.tellpal.v2.content.domain.ContentLocalization;
import com.tellpal.v2.content.domain.LocalizationStatus;
import com.tellpal.v2.content.domain.ProcessingStatus;
import com.tellpal.v2.content.domain.StoryPublicationBlocker;
import com.tellpal.v2.content.domain.StoryPublicationReadinessPolicy;
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
    private final StoryPublicationReadinessPolicy storyReadinessPolicy = new StoryPublicationReadinessPolicy();

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

    @Override
    public AdminContentRegistryPage listRegistry(
            LanguageCode languageCode,
            ContentApiType type,
            AdminContentRegistryReadiness readiness,
            String query,
            int page,
            int size) {
        LanguageCode requiredLanguage = requireLanguageCode(languageCode);
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Registry page must be non-negative and size must be between 1 and 100");
        }
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase();
        List<AdminContentRegistryItem> allItems = contentRepository.findAllForRegistryRead().stream()
                .map(content -> toRegistryItem(content, requiredLanguage))
                .filter(item -> type == null || item.type() == type)
                .filter(item -> readiness == null || item.readiness() == readiness)
                .filter(item -> matchesRegistryQuery(item, normalizedQuery))
                .sorted(Comparator.comparing(AdminContentRegistryItem::lastEditedAt).reversed()
                        .thenComparing(AdminContentRegistryItem::contentId, Comparator.reverseOrder()))
                .toList();
        int fromIndex = Math.min(page * size, allItems.size());
        int toIndex = Math.min(fromIndex + size, allItems.size());
        return new AdminContentRegistryPage(allItems.subList(fromIndex, toIndex), page, size, allItems.size());
    }

    private AdminContentRegistryItem toRegistryItem(Content content, LanguageCode languageCode) {
        List<AdminContentRegistryBlocker> blockers = content.getType().supportsStoryPages()
                ? storyReadinessPolicy.evaluate(content, languageCode).stream()
                        .map(this::toRegistryBlocker)
                        .collect(java.util.stream.Collectors.toCollection(ArrayList::new))
                : nonStoryBlockers(content, languageCode);
        ContentLocalization localization = content.findLocalization(languageCode).orElse(null);
        if (content.getType().supportsStoryPages()
                && localization != null
                && localization.getProcessingStatus() != ProcessingStatus.COMPLETED) {
            blockers.add(new AdminContentRegistryBlocker("PROCESSING_NOT_COMPLETED", null));
        }
        AdminContentRegistryReadiness readiness = !blockers.isEmpty()
                ? AdminContentRegistryReadiness.ACTION_REQUIRED
                : localization != null && localization.isVisibleToMobile()
                        ? AdminContentRegistryReadiness.PUBLISHED
                        : AdminContentRegistryReadiness.READY_TO_PUBLISH;
        return new AdminContentRegistryItem(
                content.getId(),
                ContentApiType.valueOf(content.getType().name()),
                content.getExternalKey(),
                content.getPageCount(),
                languageCode,
                localization == null ? null : localization.getTitle(),
                readiness,
                blockers,
                lastEditedAt(content, languageCode));
    }

    private List<AdminContentRegistryBlocker> nonStoryBlockers(Content content, LanguageCode languageCode) {
        List<AdminContentRegistryBlocker> blockers = new ArrayList<>();
        if (!content.isActive()) {
            blockers.add(new AdminContentRegistryBlocker("CONTENT_INACTIVE", null));
        }
        ContentLocalization localization = content.findLocalization(languageCode).orElse(null);
        if (localization == null) {
            blockers.add(new AdminContentRegistryBlocker("LOCALIZATION_MISSING", null));
        } else if (localization.getProcessingStatus() != ProcessingStatus.COMPLETED) {
            blockers.add(new AdminContentRegistryBlocker("PROCESSING_NOT_COMPLETED", null));
        }
        return List.copyOf(blockers);
    }

    private AdminContentRegistryBlocker toRegistryBlocker(StoryPublicationBlocker blocker) {
        return new AdminContentRegistryBlocker(blocker.code().name(), blocker.pageNumber());
    }

    private static boolean matchesRegistryQuery(AdminContentRegistryItem item, String normalizedQuery) {
        return normalizedQuery.isEmpty()
                || item.contentId().toString().contains(normalizedQuery)
                || item.externalKey().toLowerCase().contains(normalizedQuery)
                || item.title() != null && item.title().toLowerCase().contains(normalizedQuery);
    }

    private static java.time.Instant lastEditedAt(Content content, LanguageCode languageCode) {
        return java.util.stream.Stream.concat(
                        java.util.stream.Stream.of(content.getUpdatedAt()),
                        java.util.stream.Stream.concat(
                                content.findLocalization(languageCode).stream().map(ContentLocalization::getUpdatedAt),
                                content.getStoryPages().stream().flatMap(page -> java.util.stream.Stream.concat(
                                        java.util.stream.Stream.of(page.getUpdatedAt()),
                                        page.findLocalization(languageCode).stream().map(localization -> localization.getUpdatedAt())))))
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElseThrow(() -> new IllegalStateException("Persisted content must have an updated timestamp"));
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
