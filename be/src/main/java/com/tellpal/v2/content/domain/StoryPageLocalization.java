package com.tellpal.v2.content.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.tellpal.v2.shared.domain.LanguageCode;
import com.tellpal.v2.shared.infrastructure.persistence.BaseJpaEntity;

/**
 * Localized story-page content owned by a single story page.
 *
 * <p>Each localization carries body text, audio, and illustration assets for one language.
 */
@Entity
@Table(name = "story_page_localizations")
public class StoryPageLocalization extends BaseJpaEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "story_page_id", nullable = false)
    private StoryPage storyPage;

    @Column(name = "language_code", nullable = false, length = 8)
    private LanguageCode languageCode;

    @Column(name = "body_text")
    private String bodyText;

    @Column(name = "audio_media_id")
    private Long audioMediaId;

    @Column(name = "illustration_media_id", nullable = false)
    private Long illustrationMediaId;

    protected StoryPageLocalization() {
    }

    StoryPageLocalization(
            StoryPage storyPage,
            LanguageCode languageCode,
            String bodyText,
            Long audioMediaId,
            Long illustrationMediaId) {
        this.storyPage = requireStoryPage(storyPage);
        this.languageCode = requireLanguageCode(languageCode);
        update(bodyText, audioMediaId, illustrationMediaId);
    }

    public LanguageCode getLanguageCode() {
        return languageCode;
    }

    public String getBodyText() {
        return bodyText;
    }

    public Long getAudioMediaId() {
        return audioMediaId;
    }

    public Long getIllustrationMediaId() {
        return illustrationMediaId;
    }

    /**
     * Replaces localized body text plus audio and illustration metadata for this page.
     */
    public void update(String bodyText, Long audioMediaId, Long illustrationMediaId) {
        this.bodyText = normalizeOptionalText(bodyText);
        this.audioMediaId = normalizePositiveId(audioMediaId);
        this.illustrationMediaId = requirePositiveId(
                illustrationMediaId,
                "Illustration media ID must be positive");
    }

    private static StoryPage requireStoryPage(StoryPage storyPage) {
        if (storyPage == null) {
            throw new IllegalArgumentException("Story page must not be null");
        }
        return storyPage;
    }

    private static LanguageCode requireLanguageCode(LanguageCode languageCode) {
        if (languageCode == null) {
            throw new IllegalArgumentException("Language code must not be null");
        }
        return languageCode;
    }

    private static String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static Long normalizePositiveId(Long value) {
        if (value == null) {
            return null;
        }
        if (value <= 0) {
            throw new IllegalArgumentException("Audio media ID must be positive");
        }
        return value;
    }

    private static Long requirePositiveId(Long value, String message) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
