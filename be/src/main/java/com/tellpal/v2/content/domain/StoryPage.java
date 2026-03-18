package com.tellpal.v2.content.domain;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import com.tellpal.v2.shared.domain.LanguageCode;
import com.tellpal.v2.shared.infrastructure.persistence.BaseJpaEntity;

/**
 * Story-owned page aggregate member and owner of localized page content.
 */
@Entity
@Table(name = "story_pages")
public class StoryPage extends BaseJpaEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "content_id", nullable = false)
    private Content content;

    @Column(name = "page_number", nullable = false)
    private int pageNumber;

    @Column(name = "illustration_media_id")
    private Long illustrationMediaId;

    @OneToMany(mappedBy = "storyPage", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<StoryPageLocalization> localizations = new LinkedHashSet<>();

    protected StoryPage() {
    }

    StoryPage(Content content, int pageNumber, Long illustrationMediaId) {
        this.content = requireContent(content);
        this.pageNumber = requirePositive(pageNumber, "Story page number must be positive");
        this.illustrationMediaId = normalizePositiveId(illustrationMediaId, "Illustration media ID must be positive");
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public Long getIllustrationMediaId() {
        return illustrationMediaId;
    }

    public Set<StoryPageLocalization> getLocalizations() {
        return Collections.unmodifiableSet(localizations);
    }

    public Optional<StoryPageLocalization> findLocalization(LanguageCode languageCode) {
        LanguageCode requiredLanguageCode = requireLanguageCode(languageCode);
        return localizations.stream()
                .filter(candidate -> candidate.getLanguageCode() == requiredLanguageCode)
                .findFirst();
    }

    public void updateIllustrationMediaId(Long illustrationMediaId) {
        this.illustrationMediaId = normalizePositiveId(
                illustrationMediaId,
                "Illustration media ID must be positive");
    }

    /**
     * Creates or updates localized body and audio content for this page.
     */
    public StoryPageLocalization upsertLocalization(LanguageCode languageCode, String bodyText, Long audioMediaId) {
        StoryPageLocalization localization = findLocalization(languageCode)
                .orElseGet(() -> createLocalization(languageCode));
        localization.update(bodyText, audioMediaId);
        return localization;
    }

    private StoryPageLocalization createLocalization(LanguageCode languageCode) {
        StoryPageLocalization localization = new StoryPageLocalization(this, languageCode);
        localizations.add(localization);
        return localization;
    }

    private static Content requireContent(Content content) {
        if (content == null) {
            throw new IllegalArgumentException("Content must not be null");
        }
        return content;
    }

    private static LanguageCode requireLanguageCode(LanguageCode languageCode) {
        if (languageCode == null) {
            throw new IllegalArgumentException("Language code must not be null");
        }
        return languageCode;
    }

    private static int requirePositive(int value, String message) {
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static Long normalizePositiveId(Long value, String message) {
        if (value == null) {
            return null;
        }
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
