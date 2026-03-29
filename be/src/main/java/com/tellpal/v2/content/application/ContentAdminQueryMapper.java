package com.tellpal.v2.content.application;

import java.util.Comparator;

import com.tellpal.v2.content.api.AdminContentLocalizationView;
import com.tellpal.v2.content.api.AdminContentView;
import com.tellpal.v2.content.api.ContentApiType;
import com.tellpal.v2.content.domain.Content;
import com.tellpal.v2.content.domain.ContentLocalization;

final class ContentAdminQueryMapper {

    private ContentAdminQueryMapper() {
    }

    static AdminContentView toView(Content content) {
        Long contentId = requireContentId(content);
        return new AdminContentView(
                contentId,
                ContentApiType.valueOf(content.getType().name()),
                content.getExternalKey(),
                content.isActive(),
                content.getAgeRange(),
                content.getPageCount(),
                content.getLocalizations().stream()
                        .sorted(Comparator.comparing(localization -> localization.getLanguageCode().value()))
                        .map(localization -> toLocalizationView(contentId, localization))
                        .toList());
    }

    private static AdminContentLocalizationView toLocalizationView(Long contentId, ContentLocalization localization) {
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
                localization.isVisibleToMobile());
    }

    private static Long requireContentId(Content content) {
        Long contentId = content.getId();
        if (contentId == null || contentId <= 0) {
            throw new IllegalStateException("Content must be persisted before admin query mapping");
        }
        return contentId;
    }
}
