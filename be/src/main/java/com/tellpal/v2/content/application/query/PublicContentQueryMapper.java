package com.tellpal.v2.content.application.query;

import com.tellpal.v2.asset.api.AssetRecord;
import com.tellpal.v2.asset.api.ContentDeliveryAssets;
import com.tellpal.v2.asset.api.ResolvedAssetReference;
import com.tellpal.v2.content.api.ContentApiType;
import com.tellpal.v2.content.api.PublicContentDetails;
import com.tellpal.v2.content.api.PublicContentSummary;
import com.tellpal.v2.content.api.PublicStoryPage;
import com.tellpal.v2.content.domain.Content;
import com.tellpal.v2.content.domain.ContentLocalization;
import com.tellpal.v2.content.domain.StoryPage;
import com.tellpal.v2.content.domain.StoryPageLocalization;
import com.tellpal.v2.shared.domain.LanguageCode;

final class PublicContentQueryMapper {

    private PublicContentQueryMapper() {
    }

    static PublicContentSummary toSummary(
            Content content,
            ContentLocalization localization,
            boolean free,
            ContentDeliveryAssets assets) {
        return new PublicContentSummary(
                requireContentId(content),
                ContentApiType.valueOf(content.getType().name()),
                content.getExternalKey(),
                localization.getLanguageCode(),
                localization.getTitle(),
                localization.getDescription(),
                content.getAgeRange(),
                content.getPageCount(),
                localization.getDurationMinutes(),
                free,
                assets);
    }

    static PublicContentDetails toDetails(
            Content content,
            ContentLocalization localization,
            boolean free,
            ContentDeliveryAssets assets) {
        return new PublicContentDetails(
                requireContentId(content),
                ContentApiType.valueOf(content.getType().name()),
                content.getExternalKey(),
                localization.getLanguageCode(),
                localization.getTitle(),
                localization.getDescription(),
                localization.getBodyText(),
                content.getAgeRange(),
                content.getPageCount(),
                localization.getDurationMinutes(),
                localization.getPublishedAt(),
                free,
                assets);
    }

    static PublicStoryPage toStoryPage(
            Long contentId,
            LanguageCode languageCode,
            StoryPage storyPage,
            AssetRecord illustration,
            AssetRecord audio) {
        StoryPageLocalization localization = storyPage.findLocalization(languageCode).orElse(null);
        return new PublicStoryPage(
                contentId,
                storyPage.getPageNumber(),
                languageCode,
                localization == null ? null : localization.getBodyText(),
                toResolvedAssetReference(illustration),
                toResolvedAssetReference(audio));
    }

    static ResolvedAssetReference toResolvedAssetReference(AssetRecord assetRecord) {
        if (assetRecord == null) {
            return null;
        }
        return new ResolvedAssetReference(
                assetRecord.assetId(),
                assetRecord.kind(),
                assetRecord.storageLocation().provider(),
                assetRecord.storageLocation().objectPath(),
                assetRecord.mimeType(),
                assetRecord.cachedDownloadUrl(),
                assetRecord.downloadUrlExpiresAt());
    }

    private static Long requireContentId(Content content) {
        Long contentId = content.getId();
        if (contentId == null || contentId <= 0) {
            throw new IllegalStateException("Content must be persisted before public query mapping");
        }
        return contentId;
    }
}
