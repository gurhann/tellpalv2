package com.tellpal.v2.content.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
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
import com.tellpal.v2.content.domain.LocalizationStatus;
import com.tellpal.v2.content.domain.ProcessingStatus;
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
    private final ContentRegistryReadRepository contentRegistryReadRepository;

    public AdminContentQueryService(
            ContentRepository contentRepository,
            ContentRegistryReadRepository contentRegistryReadRepository) {
        this.contentRepository = contentRepository;
        this.contentRegistryReadRepository = contentRegistryReadRepository;
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
        ContentRegistryReadRepository.RegistryPage registryPage = contentRegistryReadRepository.findPage(
                new ContentRegistryReadRepository.RegistryQuery(
                        requiredLanguage,
                        type,
                        readiness,
                        normalizeRegistryQuery(query),
                        page,
                        size));
        Map<Long, List<ContentRegistryReadRepository.RegistrySnapshotRow>> rowsByContentId =
                contentRegistryReadRepository.findSnapshots(
                                registryPage.candidates().stream()
                                        .map(ContentRegistryReadRepository.RegistryCandidate::contentId)
                                        .toList(),
                                requiredLanguage)
                        .stream()
                        .collect(java.util.stream.Collectors.groupingBy(
                                ContentRegistryReadRepository.RegistrySnapshotRow::contentId));
        List<AdminContentRegistryItem> items = registryPage.candidates().stream()
                .map(candidate -> toRegistryItem(
                        candidate,
                        requiredLanguage,
                        rowsByContentId.get(candidate.contentId())))
                .toList();
        return new AdminContentRegistryPage(items, page, size, registryPage.totalItems());
    }

    private AdminContentRegistryItem toRegistryItem(
            ContentRegistryReadRepository.RegistryCandidate candidate,
            LanguageCode languageCode,
            List<ContentRegistryReadRepository.RegistrySnapshotRow> snapshotRows) {
        if (snapshotRows == null || snapshotRows.isEmpty()) {
            throw new IllegalStateException("Registry candidate must have a selected-page snapshot");
        }
        ContentRegistryReadRepository.RegistrySnapshotRow content = snapshotRows.getFirst();
        List<AdminContentRegistryBlocker> blockers = content.type() == ContentApiType.STORY
                ? storyBlockers(content, snapshotRows)
                : nonStoryBlockers(content);
        return new AdminContentRegistryItem(
                candidate.contentId(),
                content.type(),
                content.externalKey(),
                content.pageCount(),
                languageCode,
                content.title(),
                candidate.readiness(),
                blockers,
                candidate.lastEditedAt());
    }

    private static List<AdminContentRegistryBlocker> storyBlockers(
            ContentRegistryReadRepository.RegistrySnapshotRow content,
            List<ContentRegistryReadRepository.RegistrySnapshotRow> snapshotRows) {
        List<AdminContentRegistryBlocker> blockers = new ArrayList<>();
        if (!content.active()) {
            blockers.add(new AdminContentRegistryBlocker("CONTENT_INACTIVE", null));
        }
        if (content.title() == null) {
            return List.of(new AdminContentRegistryBlocker("LOCALIZATION_MISSING", null));
        }
        if (content.description() == null) {
            blockers.add(new AdminContentRegistryBlocker("DESCRIPTION_MISSING", null));
        }
        if (content.coverMediaId() == null) {
            blockers.add(new AdminContentRegistryBlocker("COVER_MISSING", null));
        }
        if (content.pageCount() == null || content.pageCount() == 0) {
            blockers.add(new AdminContentRegistryBlocker("STORY_PAGES_MISSING", null));
        }
        snapshotRows.stream()
                .filter(row -> row.storyPageNumber() != null)
                .forEach(row -> addStoryPageBlockers(blockers, row));
        if (!ProcessingStatus.COMPLETED.name().equals(content.processingStatus())) {
            blockers.add(new AdminContentRegistryBlocker("PROCESSING_NOT_COMPLETED", null));
        }
        return List.copyOf(blockers);
    }

    private static void addStoryPageBlockers(
            List<AdminContentRegistryBlocker> blockers,
            ContentRegistryReadRepository.RegistrySnapshotRow page) {
        if (page.storyPageLocalizationId() == null) {
            blockers.add(new AdminContentRegistryBlocker("PAGE_LOCALIZATION_MISSING", page.storyPageNumber()));
            return;
        }
        if (page.storyPageBodyText() == null) {
            blockers.add(new AdminContentRegistryBlocker("PAGE_TEXT_MISSING", page.storyPageNumber()));
        }
        if (page.storyPageAudioMediaId() == null) {
            blockers.add(new AdminContentRegistryBlocker("PAGE_AUDIO_MISSING", page.storyPageNumber()));
        }
        if (page.storyPageIllustrationMediaId() == null) {
            blockers.add(new AdminContentRegistryBlocker("PAGE_ILLUSTRATION_MISSING", page.storyPageNumber()));
        }
    }

    private static List<AdminContentRegistryBlocker> nonStoryBlockers(
            ContentRegistryReadRepository.RegistrySnapshotRow content) {
        List<AdminContentRegistryBlocker> blockers = new ArrayList<>();
        if (!content.active()) {
            blockers.add(new AdminContentRegistryBlocker("CONTENT_INACTIVE", null));
        }
        if (content.title() == null) {
            blockers.add(new AdminContentRegistryBlocker("LOCALIZATION_MISSING", null));
        } else if (!ProcessingStatus.COMPLETED.name().equals(content.processingStatus())) {
            blockers.add(new AdminContentRegistryBlocker("PROCESSING_NOT_COMPLETED", null));
        }
        return List.copyOf(blockers);
    }

    private static String normalizeRegistryQuery(String query) {
        return query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
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
