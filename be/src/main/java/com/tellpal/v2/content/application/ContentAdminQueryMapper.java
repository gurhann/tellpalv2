package com.tellpal.v2.content.application;

import java.util.Comparator;

import com.tellpal.v2.content.api.AdminContentLocalizationView;
import com.tellpal.v2.content.api.AdminContentView;
import com.tellpal.v2.content.api.AdminStoryNarrationView;
import com.tellpal.v2.content.api.ContentApiType;
import com.tellpal.v2.content.domain.Content;
import com.tellpal.v2.content.domain.ContentLocalization;
import com.tellpal.v2.asset.api.AssetProcessingApi;
import com.tellpal.v2.asset.api.AssetProcessingKind;

final class ContentAdminQueryMapper {

    private final AssetProcessingApi assetProcessingApi;

    ContentAdminQueryMapper(AssetProcessingApi assetProcessingApi) {
        this.assetProcessingApi = assetProcessingApi;
    }

    AdminContentView toView(Content content) {
        Long contentId = requireContentId(content);
        return new AdminContentView(
                contentId,
                ContentApiType.valueOf(content.getType().name()),
                content.getExternalKey(),
                content.isActive(),
                content.getAgeRange(),
                content.getPageCount(),
                content.getTextlessCoverMediaId(),
                content.getLocalizations().stream()
                        .sorted(Comparator.comparing(localization -> localization.getLanguageCode().value()))
                        .map(localization -> toLocalizationView(contentId, localization))
                        .toList());
    }

    private AdminContentLocalizationView toLocalizationView(Long contentId, ContentLocalization localization) {
        var narration = localization.getNarration();
        var processing = narration == null ? null : assetProcessingApi.findByTarget(
                com.tellpal.v2.asset.api.AssetProcessingTarget.localization(contentId, localization.getLanguageCode()),
                AssetProcessingKind.STORY_NARRATION).orElse(null);
        return new AdminContentLocalizationView(
                contentId,
                localization.getLanguageCode(),
                localization.getTitle(),
                localization.getDescription(),
                localization.getBodyText(),
                localization.getCoverMediaId(),
                localization.getAudioMediaId(),
                localization.getDurationMinutes(),
                localization.getStatus().name(),
                localization.getProcessingStatus().name(),
                localization.getPublishedAt(),
                localization.isVisibleToMobile(),
                narration == null ? null : new AdminStoryNarrationView(
                        narration.getAudioMediaId(), narration.getDurationMinutes(),
                        processing == null ? null : processing.status().name(),
                        processing == null ? null : processing.lastErrorMessage() != null
                                ? processing.lastErrorMessage() : processing.lastErrorCode()));
    }

    private static Long requireContentId(Content content) {
        Long contentId = content.getId();
        if (contentId == null || contentId <= 0) {
            throw new IllegalStateException("Content must be persisted before admin query mapping");
        }
        return contentId;
    }
}
