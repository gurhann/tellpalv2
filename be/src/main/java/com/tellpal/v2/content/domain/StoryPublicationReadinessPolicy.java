package com.tellpal.v2.content.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.tellpal.v2.shared.domain.LanguageCode;

/**
 * Evaluates every editorial requirement for publishing one STORY localization.
 */
public final class StoryPublicationReadinessPolicy {

    public List<StoryPublicationBlocker> evaluate(Content content, LanguageCode languageCode) {
        List<StoryPublicationBlocker> blockers = new ArrayList<>();
        if (!content.isActive()) {
            blockers.add(new StoryPublicationBlocker(StoryPublicationBlockerCode.CONTENT_INACTIVE, null));
        }
        ContentLocalization localization = content.findLocalization(languageCode).orElse(null);
        if (localization == null) {
            return List.of(new StoryPublicationBlocker(StoryPublicationBlockerCode.LOCALIZATION_MISSING, null));
        }
        if (localization.getDescription() == null) {
            blockers.add(new StoryPublicationBlocker(StoryPublicationBlockerCode.DESCRIPTION_MISSING, null));
        }
        if (localization.getCoverMediaId() == null) {
            blockers.add(new StoryPublicationBlocker(StoryPublicationBlockerCode.COVER_MISSING, null));
        }
        if (content.getStoryPages().isEmpty()) {
            blockers.add(new StoryPublicationBlocker(StoryPublicationBlockerCode.STORY_PAGES_MISSING, null));
        }
        content.getStoryPages().stream()
                .sorted(Comparator.comparingInt(StoryPage::getPageNumber))
                .forEach(page -> addPageBlockers(blockers, page, languageCode));
        return List.copyOf(blockers);
    }

    private static void addPageBlockers(
            List<StoryPublicationBlocker> blockers,
            StoryPage page,
            LanguageCode languageCode) {
        StoryPageLocalization localization = page.findLocalization(languageCode).orElse(null);
        if (localization == null) {
            blockers.add(new StoryPublicationBlocker(
                    StoryPublicationBlockerCode.PAGE_LOCALIZATION_MISSING, page.getPageNumber()));
            return;
        }
        if (localization.getBodyText() == null) {
            blockers.add(new StoryPublicationBlocker(StoryPublicationBlockerCode.PAGE_TEXT_MISSING, page.getPageNumber()));
        }
        if (localization.getAudioMediaId() == null) {
            blockers.add(new StoryPublicationBlocker(StoryPublicationBlockerCode.PAGE_AUDIO_MISSING, page.getPageNumber()));
        }
        if (localization.getIllustrationMediaId() == null) {
            blockers.add(new StoryPublicationBlocker(
                    StoryPublicationBlockerCode.PAGE_ILLUSTRATION_MISSING, page.getPageNumber()));
        }
    }
}
