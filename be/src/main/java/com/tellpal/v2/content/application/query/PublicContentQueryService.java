package com.tellpal.v2.content.application.query;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tellpal.v2.asset.api.AssetRecord;
import com.tellpal.v2.asset.api.AssetRegistryApi;
import com.tellpal.v2.asset.api.ContentAssetBundleApi;
import com.tellpal.v2.asset.api.ContentDeliveryAssets;
import com.tellpal.v2.content.api.ContentApiType;
import com.tellpal.v2.content.api.PublicContentDetails;
import com.tellpal.v2.content.api.PublicContentQueryApi;
import com.tellpal.v2.content.api.PublicContentSummary;
import com.tellpal.v2.content.api.PublicStoryPage;
import com.tellpal.v2.content.domain.Content;
import com.tellpal.v2.content.domain.ContentLocalization;
import com.tellpal.v2.content.domain.ContentRepository;
import com.tellpal.v2.content.domain.StoryPage;
import com.tellpal.v2.shared.domain.LanguageCode;

/**
 * Read-model service for mobile-facing content queries.
 *
 * <p>The service only exposes active content with mobile-visible localizations and joins generated
 * delivery assets plus free-access state into the returned views.
 */
@Service
@Transactional(readOnly = true)
public class PublicContentQueryService implements PublicContentQueryApi {

    private final ContentRepository contentRepository;
    private final com.tellpal.v2.content.api.ContentFreeAccessApi contentFreeAccessApi;
    private final ContentAssetBundleApi contentAssetBundleApi;
    private final AssetRegistryApi assetRegistryApi;

    public PublicContentQueryService(
            ContentRepository contentRepository,
            com.tellpal.v2.content.api.ContentFreeAccessApi contentFreeAccessApi,
            ContentAssetBundleApi contentAssetBundleApi,
            AssetRegistryApi assetRegistryApi) {
        this.contentRepository = contentRepository;
        this.contentFreeAccessApi = contentFreeAccessApi;
        this.contentAssetBundleApi = contentAssetBundleApi;
        this.assetRegistryApi = assetRegistryApi;
    }

    @Override
    /**
     * Lists visible content summaries for one language and optional type filter.
     */
    public List<PublicContentSummary> listContents(LanguageCode languageCode, String requestedAccessKey, ContentApiType type) {
        LanguageCode requiredLanguageCode = requireLanguageCode(languageCode);
        Set<Long> freeContentIds = resolveFreeContentIds(requiredLanguageCode, requestedAccessKey);
        Predicate<Content> typeFilter = content -> type == null || ContentApiType.valueOf(content.getType().name()) == type;
        return contentRepository.findAllActive().stream()
                .filter(typeFilter)
                .sorted(java.util.Comparator.comparing(Content::getId))
                .flatMap(content -> visibleLocalization(content, requiredLanguageCode)
                        .stream()
                        .map(localization -> toSummary(content, localization, freeContentIds)))
                .toList();
    }

    @Override
    /**
     * Returns visible content summaries in the caller-provided ID order.
     */
    public List<PublicContentSummary> listContentsByIds(
            List<Long> contentIds,
            LanguageCode languageCode,
            String requestedAccessKey) {
        if (contentIds == null || contentIds.isEmpty()) {
            return List.of();
        }
        LanguageCode requiredLanguageCode = requireLanguageCode(languageCode);
        List<Long> orderedIds = distinctOrderedIds(contentIds);
        Map<Long, Integer> orderIndex = buildOrderIndex(orderedIds);
        Set<Long> freeContentIds = resolveFreeContentIds(requiredLanguageCode, requestedAccessKey);
        return contentRepository.findAllActiveByIdIn(orderedIds).stream()
                .sorted(java.util.Comparator.comparing(content -> orderIndex.getOrDefault(content.getId(), Integer.MAX_VALUE)))
                .flatMap(content -> visibleLocalization(content, requiredLanguageCode)
                        .stream()
                        .map(localization -> toSummary(content, localization, freeContentIds)))
                .toList();
    }

    @Override
    /**
     * Returns public details for a visible content localization.
     */
    public Optional<PublicContentDetails> findContent(Long contentId, LanguageCode languageCode, String requestedAccessKey) {
        LanguageCode requiredLanguageCode = requireLanguageCode(languageCode);
        Set<Long> freeContentIds = resolveFreeContentIds(requiredLanguageCode, requestedAccessKey);
        return contentRepository.findById(requireContentId(contentId))
                .filter(Content::isActive)
                .flatMap(content -> visibleLocalization(content, requiredLanguageCode)
                        .map(localization -> PublicContentQueryMapper.toDetails(
                                content,
                                localization,
                                freeContentIds.contains(content.getId()),
                                loadAssets(content.getId(), requiredLanguageCode))));
    }

    @Override
    /**
     * Returns localized story pages for visible story content.
     */
    public Optional<List<PublicStoryPage>> findStoryPages(Long contentId, LanguageCode languageCode) {
        LanguageCode requiredLanguageCode = requireLanguageCode(languageCode);
        return contentRepository.findById(requireContentId(contentId))
                .filter(Content::isActive)
                .flatMap(content -> visibleLocalization(content, requiredLanguageCode)
                        .map(localization -> content.getStoryPages().stream()
                                .sorted(java.util.Comparator.comparingInt(StoryPage::getPageNumber))
                                .map(storyPage -> PublicContentQueryMapper.toStoryPage(
                                        content.getId(),
                                        localization.getLanguageCode(),
                                        storyPage,
                                        loadAsset(storyPage.getIllustrationMediaId()),
                                        loadAsset(storyPage.findLocalization(requiredLanguageCode)
                                                .map(com.tellpal.v2.content.domain.StoryPageLocalization::getAudioMediaId)
                                                .orElse(null))))
                                .toList()));
    }

    private PublicContentSummary toSummary(Content content, ContentLocalization localization, Set<Long> freeContentIds) {
        return PublicContentQueryMapper.toSummary(
                content,
                localization,
                freeContentIds.contains(content.getId()),
                loadAssets(content.getId(), localization.getLanguageCode()));
    }

    private ContentDeliveryAssets loadAssets(Long contentId, LanguageCode languageCode) {
        return contentAssetBundleApi.findForLocalization(contentId, languageCode)
                .orElse(ContentDeliveryAssets.empty());
    }

    private AssetRecord loadAsset(Long assetId) {
        if (assetId == null) {
            return null;
        }
        return assetRegistryApi.findById(assetId).orElse(null);
    }

    private Set<Long> resolveFreeContentIds(LanguageCode languageCode, String requestedAccessKey) {
        return contentFreeAccessApi.resolveFreeAccess(languageCode, requestedAccessKey).contentIds();
    }

    private Optional<ContentLocalization> visibleLocalization(Content content, LanguageCode languageCode) {
        return content.findLocalization(languageCode)
                .filter(ContentLocalization::isVisibleToMobile);
    }

    private static List<Long> distinctOrderedIds(List<Long> contentIds) {
        LinkedHashSet<Long> distinctIds = new LinkedHashSet<>();
        for (Long contentId : contentIds) {
            distinctIds.add(requireContentId(contentId));
        }
        return List.copyOf(distinctIds);
    }

    private static Map<Long, Integer> buildOrderIndex(List<Long> orderedIds) {
        Map<Long, Integer> orderIndex = new LinkedHashMap<>();
        for (int index = 0; index < orderedIds.size(); index++) {
            orderIndex.put(orderedIds.get(index), index);
        }
        return Collections.unmodifiableMap(orderIndex);
    }

    private static Long requireContentId(Long contentId) {
        if (contentId == null || contentId <= 0) {
            throw new IllegalArgumentException("Content ID must be positive");
        }
        return contentId;
    }

    private static LanguageCode requireLanguageCode(LanguageCode languageCode) {
        if (languageCode == null) {
            throw new IllegalArgumentException("Language code must not be null");
        }
        return languageCode;
    }
}
