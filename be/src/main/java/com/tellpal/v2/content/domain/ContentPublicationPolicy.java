package com.tellpal.v2.content.domain;

import java.time.Instant;

import com.tellpal.v2.shared.domain.LanguageCode;

public final class ContentPublicationPolicy {

    public void publish(Content content, ContentLocalization localization, Instant publishedAt) {
        Content requiredContent = requireContent(content);
        ContentLocalization requiredLocalization = requireLocalization(localization);
        Instant requiredPublishedAt = requirePublishedAt(publishedAt);
        ensureStoryReadyForPublication(requiredContent, requiredLocalization.getLanguageCode());
        requiredLocalization.markStatus(LocalizationStatus.PUBLISHED, requiredPublishedAt);
    }

    public void archive(ContentLocalization localization) {
        requireLocalization(localization).markStatus(LocalizationStatus.ARCHIVED, localization.getPublishedAt());
    }

    private void ensureStoryReadyForPublication(Content content, LanguageCode languageCode) {
        if (!content.getType().supportsStoryPages()) {
            return;
        }
        if (content.getStoryPages().isEmpty()) {
            throw new IllegalStateException("Story content must include at least one story page before publication");
        }
        for (StoryPage storyPage : content.getStoryPages()) {
            StoryPageLocalization localization = storyPage.findLocalization(languageCode)
                    .orElseThrow(() -> new IllegalStateException(
                            "Story page %d must have localization for %s before publication"
                                    .formatted(storyPage.getPageNumber(), languageCode.value())));
            if (localization.getBodyText() == null) {
                throw new IllegalStateException(
                        "Story page %d must include body text for %s before publication"
                                .formatted(storyPage.getPageNumber(), languageCode.value()));
            }
            if (localization.getAudioMediaId() == null) {
                throw new IllegalStateException(
                        "Story page %d must include audio media for %s before publication"
                                .formatted(storyPage.getPageNumber(), languageCode.value()));
            }
        }
    }

    private static Content requireContent(Content content) {
        if (content == null) {
            throw new IllegalArgumentException("Content must not be null");
        }
        return content;
    }

    private static ContentLocalization requireLocalization(ContentLocalization localization) {
        if (localization == null) {
            throw new IllegalArgumentException("Content localization must not be null");
        }
        return localization;
    }

    private static Instant requirePublishedAt(Instant publishedAt) {
        if (publishedAt == null) {
            throw new IllegalArgumentException("Published timestamp must not be null");
        }
        return publishedAt;
    }
}
