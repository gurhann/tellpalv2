package com.tellpal.v2.content.application;

import java.util.Comparator;

import com.tellpal.v2.content.api.AdminStoryPageLocalizationView;
import com.tellpal.v2.content.api.AdminStoryPageView;
import com.tellpal.v2.content.domain.StoryPage;
import com.tellpal.v2.content.domain.StoryPageLocalization;

final class StoryPageAdminQueryMapper {

    private StoryPageAdminQueryMapper() {
    }

    static AdminStoryPageView toView(Long contentId, StoryPage storyPage) {
        return new AdminStoryPageView(
                contentId,
                storyPage.getPageNumber(),
                storyPage.getLocalizations().size(),
                storyPage.getLocalizations().stream()
                        .sorted(Comparator.comparing(localization -> localization.getLanguageCode().value()))
                        .map(localization -> toLocalizationView(contentId, storyPage.getPageNumber(), localization))
                        .toList());
    }

    private static AdminStoryPageLocalizationView toLocalizationView(
            Long contentId,
            int pageNumber,
            StoryPageLocalization localization) {
        return new AdminStoryPageLocalizationView(
                contentId,
                pageNumber,
                localization.getLanguageCode(),
                localization.getBodyText(),
                localization.getAudioMediaId(),
                localization.getIllustrationMediaId());
    }
}
