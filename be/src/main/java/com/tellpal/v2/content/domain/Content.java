package com.tellpal.v2.content.domain;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import com.tellpal.v2.shared.domain.LanguageCode;
import com.tellpal.v2.shared.infrastructure.persistence.BaseJpaEntity;

/**
 * Aggregate root for content identity, localizations, story pages, and contributor assignments.
 *
 * <p>The aggregate owns type-specific localization rules, story page count synchronization, and
 * contributor uniqueness per role and language.
 */
@Entity
@Table(name = "contents")
public class Content extends BaseJpaEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private ContentType type;

    @Column(name = "external_key", nullable = false, length = 180)
    private String externalKey;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "age_range")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer ageRange;

    @Column(name = "page_count")
    private Integer pageCount;

    @OneToMany(mappedBy = "content", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ContentLocalization> localizations = new LinkedHashSet<>();

    @OneToMany(mappedBy = "content", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<StoryPage> storyPages = new LinkedHashSet<>();

    @OneToMany(mappedBy = "content", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ContentContributor> contributors = new LinkedHashSet<>();

    protected Content() {
    }

    private Content(ContentType type, String externalKey, Integer ageRange, boolean active) {
        this.type = requireType(type);
        this.externalKey = requireText(externalKey, "Content external key must not be blank");
        this.active = active;
        this.ageRange = normalizeAgeRange(ageRange);
        this.pageCount = type.supportsStoryPages() ? 0 : null;
    }

    public static Content create(ContentType type, String externalKey, Integer ageRange) {
        return create(type, externalKey, ageRange, true);
    }

    public static Content create(ContentType type, String externalKey, Integer ageRange, boolean active) {
        return new Content(type, externalKey, ageRange, active);
    }

    public ContentType getType() {
        return type;
    }

    public String getExternalKey() {
        return externalKey;
    }

    public boolean isActive() {
        return active;
    }

    public Integer getAgeRange() {
        return ageRange;
    }

    public Integer getPageCount() {
        return pageCount;
    }

    public Set<ContentLocalization> getLocalizations() {
        return Collections.unmodifiableSet(localizations);
    }

    public Set<StoryPage> getStoryPages() {
        return Collections.unmodifiableSet(storyPages);
    }

    public Set<ContentContributor> getContributors() {
        return Collections.unmodifiableSet(contributors);
    }

    public Optional<ContentLocalization> findLocalization(LanguageCode languageCode) {
        LanguageCode requiredLanguageCode = requireLanguageCode(languageCode);
        return localizations.stream()
                .filter(candidate -> candidate.getLanguageCode() == requiredLanguageCode)
                .findFirst();
    }

    public Optional<StoryPage> findStoryPage(int pageNumber) {
        if (pageNumber <= 0) {
            throw new IllegalArgumentException("Story page number must be positive");
        }
        return storyPages.stream()
                .filter(candidate -> candidate.getPageNumber() == pageNumber)
                .findFirst();
    }

    public void updateDetails(String externalKey, Integer ageRange, boolean active) {
        this.externalKey = requireText(externalKey, "Content external key must not be blank");
        this.ageRange = normalizeAgeRange(ageRange);
        this.active = active;
    }

    /**
     * Deactivates the aggregate without removing localized or editorial history.
     */
    public void deactivate() {
        this.active = false;
    }

    /**
     * Creates or updates one localization while enforcing content-type-specific field rules.
     *
     * <p>Story localizations cannot carry body text or a single audio asset, while non-story types
     * may require them.
     */
    public ContentLocalization upsertLocalization(
            LanguageCode languageCode,
            String title,
            String description,
            String bodyText,
            Long coverMediaId,
            Long audioMediaId,
            Integer durationMinutes,
            LocalizationStatus status,
            ProcessingStatus processingStatus,
            java.time.Instant publishedAt) {
        validateLocalizationFieldsForType(bodyText, audioMediaId);
        ContentLocalization localization = findLocalization(languageCode)
                .orElseGet(() -> createLocalization(languageCode, title, status, processingStatus));
        localization.updateContent(title, description, bodyText, coverMediaId, audioMediaId, durationMinutes);
        localization.markStatus(status, publishedAt);
        localization.markProcessingStatus(processingStatus);
        return localization;
    }

    /**
     * Adds a story page and updates aggregate-owned page count.
     */
    public StoryPage addStoryPage(Integer afterPageNumber) {
        ensureStoryType();
        List<StoryPage> orderedStoryPages = getOrderedStoryPages();
        int insertIndex = orderedStoryPages.size();

        if (afterPageNumber != null) {
            insertIndex = indexOfPage(orderedStoryPages, afterPageNumber);
            if (insertIndex < 0) {
                throw new IllegalArgumentException("Story page not found: " + afterPageNumber);
            }
            insertIndex += 1;
        }

        StoryPage storyPage = new StoryPage(this, orderedStoryPages.size() + 1);
        storyPages.add(storyPage);
        orderedStoryPages.add(insertIndex, storyPage);
        renumberStoryPages(orderedStoryPages);
        return storyPage;
    }

    /**
     * Adds a story page at an explicit page number without renumbering the remaining pages.
     */
    public StoryPage addStoryPageAt(int pageNumber) {
        ensureStoryType();
        StoryPage storyPage = new StoryPage(this, pageNumber);
        storyPages.add(storyPage);
        syncPageCount();
        return storyPage;
    }

    /**
     * Applies a temporary positive offset to pages after the supplied page number.
     */
    public void offsetStoryPagesAfter(int pageNumber, int offset) {
        ensureStoryType();
        if (offset <= 0) {
            throw new IllegalArgumentException("Story page offset must be positive");
        }
        for (StoryPage storyPage : storyPages) {
            if (storyPage.getPageNumber() > pageNumber) {
                storyPage.renumber(storyPage.getPageNumber() + offset);
            }
        }
    }

    /**
     * Renumbers all story pages so the aggregate stays contiguous from page 1.
     */
    public void renumberStoryPagesContiguously() {
        ensureStoryType();
        renumberStoryPages(getOrderedStoryPages());
    }

    /**
     * Removes a story page and updates aggregate-owned page count.
     */
    public void removeStoryPage(int pageNumber) {
        ensureStoryType();
        List<StoryPage> orderedStoryPages = getOrderedStoryPages();
        int removedIndex = indexOfPage(orderedStoryPages, pageNumber);
        if (removedIndex < 0) {
            throw new IllegalArgumentException("Story page not found: " + pageNumber);
        }
        StoryPage removedStoryPage = orderedStoryPages.remove(removedIndex);
        storyPages.remove(removedStoryPage);
        renumberStoryPages(orderedStoryPages);
    }

    /**
     * Removes a story page without renumbering the remaining pages.
     */
    public void removeStoryPageOnly(int pageNumber) {
        ensureStoryType();
        StoryPage removedStoryPage = findStoryPage(pageNumber)
                .orElseThrow(() -> new IllegalArgumentException("Story page not found: " + pageNumber));
        storyPages.remove(removedStoryPage);
        syncPageCount();
    }

    /**
     * Assigns a contributor when the role and scope combination is not already present.
     *
     * <p>A {@code null} language code represents a global credit that applies to all localizations.
     * Sort order is unique per role and language scope inside the aggregate.
     */
    public ContentContributor assignContributor(
            Contributor contributor,
            ContributorRole role,
            LanguageCode languageCode,
            String creditName,
            int sortOrder) {
        Long contributorId = requireContributorId(contributor);
        if (contributors.stream()
                .anyMatch(assignment -> assignment.matchesAssignment(contributorId, role, languageCode))) {
            throw new IllegalArgumentException("Contributor assignment already exists for role and language");
        }
        requireContributorSortOrderAvailability(role, languageCode, sortOrder);
        ContentContributor assignment = new ContentContributor(
                this,
                contributor,
                role,
                languageCode,
                creditName,
                sortOrder);
        contributors.add(assignment);
        return assignment;
    }

    /**
     * Removes one contributor assignment identified by contributor, role, and optional language.
     */
    public void unassignContributor(Long contributorId, ContributorRole role, LanguageCode languageCode) {
        requirePositiveContributorId(contributorId);
        requireRole(role);
        boolean removed = contributors.removeIf(assignment ->
                assignment.matchesAssignment(contributorId, role, languageCode));
        if (!removed) {
            throw new IllegalArgumentException("Contributor assignment not found for role and language");
        }
    }

    private ContentLocalization createLocalization(
            LanguageCode languageCode,
            String title,
            LocalizationStatus status,
            ProcessingStatus processingStatus) {
        ContentLocalization localization = new ContentLocalization(
                this,
                languageCode,
                title,
                status,
                processingStatus);
        localizations.add(localization);
        return localization;
    }

    private void syncPageCount() {
        pageCount = storyPages.size();
    }

    private List<StoryPage> getOrderedStoryPages() {
        return storyPages.stream()
                .sorted(Comparator.comparingInt(StoryPage::getPageNumber))
                .collect(java.util.stream.Collectors.toList());
    }

    private void renumberStoryPages(List<StoryPage> orderedStoryPages) {
        for (int index = 0; index < orderedStoryPages.size(); index += 1) {
            orderedStoryPages.get(index).renumber(index + 1);
        }
        syncPageCount();
    }

    private static int indexOfPage(List<StoryPage> orderedStoryPages, int pageNumber) {
        for (int index = 0; index < orderedStoryPages.size(); index += 1) {
            if (orderedStoryPages.get(index).getPageNumber() == pageNumber) {
                return index;
            }
        }
        return -1;
    }

    private void validateLocalizationFieldsForType(String bodyText, Long audioMediaId) {
        boolean hasBodyText = bodyText != null && !bodyText.isBlank();
        boolean hasAudioMedia = audioMediaId != null;
        if (type == ContentType.STORY) {
            if (hasBodyText) {
                throw new IllegalArgumentException("Story localizations must not store body text");
            }
            if (hasAudioMedia) {
                throw new IllegalArgumentException("Story localizations must not store a single audio media reference");
            }
            return;
        }
        if ((type == ContentType.AUDIO_STORY || type == ContentType.MEDITATION) && !hasBodyText) {
            throw new IllegalArgumentException("Body text is required for audio stories and meditations");
        }
        if ((type == ContentType.AUDIO_STORY || type == ContentType.MEDITATION || type == ContentType.LULLABY)
                && !hasAudioMedia) {
            throw new IllegalArgumentException("Audio media is required for non-story content localizations");
        }
    }

    private void ensureStoryType() {
        if (!type.supportsStoryPages()) {
            throw new IllegalStateException("Story pages can only be managed for STORY content");
        }
    }

    private void requireContributorSortOrderAvailability(
            ContributorRole role,
            LanguageCode languageCode,
            int sortOrder) {
        requireRole(role);
        if (sortOrder < 0) {
            throw new IllegalArgumentException("Contributor sort order must not be negative");
        }
        boolean alreadyUsed = contributors.stream()
                .anyMatch(assignment -> assignment.matchesRoleAndLanguage(role, languageCode)
                        && assignment.getSortOrder() == sortOrder);
        if (alreadyUsed) {
            throw new IllegalArgumentException("Contributor sort order must be unique per role and language");
        }
    }

    private static ContentType requireType(ContentType type) {
        if (type == null) {
            throw new IllegalArgumentException("Content type must not be null");
        }
        return type;
    }

    private static ContributorRole requireRole(ContributorRole role) {
        if (role == null) {
            throw new IllegalArgumentException("Contributor role must not be null");
        }
        return role;
    }

    private static Long requireContributorId(Contributor contributor) {
        Long contributorId = contributor.getId();
        return requirePositiveContributorId(contributorId);
    }

    private static Long requirePositiveContributorId(Long contributorId) {
        if (contributorId == null || contributorId <= 0) {
            throw new IllegalStateException("Contributor must be persisted before content assignment");
        }
        return contributorId;
    }

    private static LanguageCode requireLanguageCode(LanguageCode languageCode) {
        if (languageCode == null) {
            throw new IllegalArgumentException("Language code must not be null");
        }
        return languageCode;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static Integer normalizeAgeRange(Integer ageRange) {
        if (ageRange == null) {
            return null;
        }
        if (ageRange < 0) {
            throw new IllegalArgumentException("Content age range must not be negative");
        }
        return ageRange;
    }
}
