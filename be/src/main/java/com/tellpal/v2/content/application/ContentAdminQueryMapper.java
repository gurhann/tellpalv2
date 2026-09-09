package com.tellpal.v2.content.application;

import java.util.Comparator;

import com.tellpal.v2.content.api.AdminContentLocalizationView;
import com.tellpal.v2.content.api.AdminContentView;
import com.tellpal.v2.content.api.AdminStoryNarrationView;
import com.tellpal.v2.content.api.AdminLullabyPlaybackView;
import com.tellpal.v2.content.api.AdminLullabyInstrumentView;
import com.tellpal.v2.content.api.ContentApiType;
import com.tellpal.v2.content.domain.Content;
import com.tellpal.v2.content.domain.ContentLocalization;
import com.tellpal.v2.content.domain.ContentType;
import com.tellpal.v2.asset.api.AssetProcessingApi;
import com.tellpal.v2.asset.api.AssetProcessingKind;

final class ContentAdminQueryMapper {

    private final AssetProcessingApi assetProcessingApi;

    ContentAdminQueryMapper(AssetProcessingApi assetProcessingApi) {
        this.assetProcessingApi = assetProcessingApi;
    }

    AdminContentView toView(Content content) {
        Long contentId = requireContentId(content);
        var playback = content.getLullabyPlayback();
        var processing = playback == null ? null : assetProcessingApi.findByContent(contentId).orElse(null);
        return new AdminContentView(
                contentId,
                ContentApiType.valueOf(content.getType().name()),
                content.getExternalKey(),
                content.isActive(),
                content.getAgeRange(),
                content.getPageCount(),
                content.getTextlessCoverMediaId(),
                content.getListeningCoverMediaId(),
                content.getListingCoverMediaId(),
                playback == null ? null : new AdminLullabyPlaybackView(
                        playback.getAudioMediaId(),
                        playback.getDurationMinutes(),
                        processing == null ? null : processing.status().name(),
                        processing == null ? null : processing.lastErrorMessage() != null
                                ? processing.lastErrorMessage() : processing.lastErrorCode(),
                        content.getOrderedLullabyInstruments().stream()
                                .map(instrument -> new AdminLullabyInstrumentView(
                                        requireCatalogId(instrument.getInstrumentCatalog()),
                                        instrument.getInstrumentCatalog().getCode(),
                                        null,
                                        instrument.getDisplayOrder()))
                                .toList()),
                content.getLocalizations().stream()
                        .sorted(Comparator.comparing(localization -> localization.getLanguageCode().value()))
                        .map(localization -> toLocalizationView(content, contentId, localization, processing))
                        .toList());
    }

    private AdminContentLocalizationView toLocalizationView(
            Content content,
            Long contentId,
            ContentLocalization localization,
            com.tellpal.v2.asset.api.AssetProcessingRecord sharedProcessing) {
        var narration = localization.getNarration();
        var processing = narration == null ? null : assetProcessingApi.findByTarget(
                com.tellpal.v2.asset.api.AssetProcessingTarget.localization(contentId, localization.getLanguageCode()),
                AssetProcessingKind.STORY_NARRATION).orElse(null);
        var effectiveProcessing = content.getType() == ContentType.LULLABY && sharedProcessing != null
                ? sharedProcessing.status().name()
                : localization.getProcessingStatus().name();
        var visibleToMobile = content.getType() == ContentType.LULLABY && sharedProcessing != null
                ? localization.isVisibleToMobile(
                        com.tellpal.v2.content.domain.ProcessingStatus.valueOf(effectiveProcessing))
                : localization.isVisibleToMobile();
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
                effectiveProcessing,
                localization.getPublishedAt(),
                visibleToMobile,
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

    private static Long requireCatalogId(com.tellpal.v2.content.domain.InstrumentCatalog catalog) {
        Long catalogId = catalog.getId();
        if (catalogId == null || catalogId <= 0) {
            throw new IllegalStateException("Instrument catalog must be persisted before admin query mapping");
        }
        return catalogId;
    }
}
