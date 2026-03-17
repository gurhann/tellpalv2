package com.tellpal.v2.content.domain;

import java.util.Collections;
import java.util.LinkedHashSet;
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

    public StoryPage addStoryPage(int pageNumber, Long illustrationMediaId) {
        ensureStoryType();
        if (storyPages.stream().anyMatch(page -> page.getPageNumber() == pageNumber)) {
            throw new IllegalArgumentException("Story page number must be unique within a content item");
        }
        StoryPage storyPage = new StoryPage(this, pageNumber, illustrationMediaId);
        storyPages.add(storyPage);
        syncPageCount();
        return storyPage;
    }

    public void removeStoryPage(int pageNumber) {
        ensureStoryType();
        boolean removed = storyPages.removeIf(page -> page.getPageNumber() == pageNumber);
        if (!removed) {
            throw new IllegalArgumentException("Story page not found: " + pageNumber);
        }
        syncPageCount();
    }

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
