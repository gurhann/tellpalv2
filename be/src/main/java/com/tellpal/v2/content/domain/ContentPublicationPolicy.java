package com.tellpal.v2.content.domain;

import java.time.Instant;
import java.util.List;

import com.tellpal.v2.shared.domain.LanguageCode;

/**
 * Domain policy for publishing and archiving content localizations.
 *
 * <p>Story publication requires at least one page and complete page localizations for the target
 * language.
 */
public final class ContentPublicationPolicy {

    private final StoryPublicationReadinessPolicy storyReadinessPolicy = new StoryPublicationReadinessPolicy();

    /**
     * Publishes a localization after validating type-specific readiness rules.
     */
    public void publish(Content content, ContentLocalization localization, Instant publishedAt) {
        Content requiredContent = requireContent(content);
        ContentLocalization requiredLocalization = requireLocalization(localization);
        Instant requiredPublishedAt = requirePublishedAt(publishedAt);
        ensureMeditationHasBody(requiredContent, requiredLocalization, "publication");
        ensureStoryReadyForPublication(requiredContent, requiredLocalization.getLanguageCode());
        requiredLocalization.markStatus(LocalizationStatus.PUBLISHED, requiredPublishedAt);
    }

    /**
     * Archives a localization while preserving its existing publish timestamp.
     */
    public void archive(Content content, ContentLocalization localization) {
        Content requiredContent = requireContent(content);
        ContentLocalization requiredLocalization = requireLocalization(localization);
        ensureMeditationHasBody(requiredContent, requiredLocalization, "archive");
        requiredLocalization.markStatus(LocalizationStatus.ARCHIVED, requiredLocalization.getPublishedAt());
    }

    private void ensureStoryReadyForPublication(Content content, LanguageCode languageCode) {
        if (!content.getType().supportsStoryPages()) {
            return;
        }
        List<StoryPublicationBlocker> blockers = storyReadinessPolicy.evaluate(content, languageCode);
        if (!blockers.isEmpty()) {
            StoryPublicationBlocker blocker = blockers.getFirst();
            throw new IllegalStateException("Story publication requirements are incomplete: "
                    + publicationMessage(blocker));
        }
    }

    private void ensureMeditationHasBody(
            Content content, ContentLocalization localization, String operation) {
        if (content.getType() != ContentType.MEDITATION) {
            return;
        }
        if (localization.getBodyText() == null || localization.getBodyText().isBlank()) {
            throw new IllegalStateException(
                    "Meditation " + operation + " requires non-blank body text");
        }
    }

    private static String publicationMessage(StoryPublicationBlocker blocker) {
        return switch (blocker.code()) {
            case PAGE_AUDIO_MISSING -> "audio media is missing";
            case PAGE_ILLUSTRATION_MISSING -> "illustration media is missing";
            case PAGE_TEXT_MISSING -> "body text is missing";
            default -> blocker.code().name();
        };
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
