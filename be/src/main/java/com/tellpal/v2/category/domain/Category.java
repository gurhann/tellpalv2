package com.tellpal.v2.category.domain;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

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
 * Aggregate root for category identity, localized presentation, and curated content ordering.
 *
 * <p>Curated content is owned per language and display order must remain unique within that
 * language scope.
 */
@Entity
@Table(name = "categories")
public class Category extends BaseJpaEntity {

    @Column(name = "slug", nullable = false, length = 180)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private CategoryType type;

    @Column(name = "is_premium", nullable = false)
    private boolean premium;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<CategoryLocalization> localizations = new LinkedHashSet<>();

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<CategoryContent> curatedContents = new LinkedHashSet<>();

    protected Category() {
    }

    private Category(String slug, CategoryType type, boolean premium, boolean active) {
        this.slug = requireText(slug, "Category slug must not be blank");
        this.type = requireType(type);
        this.premium = premium;
        this.active = active;
    }

    public static Category create(String slug, CategoryType type, boolean premium, boolean active) {
        return new Category(slug, type, premium, active);
    }

    public String getSlug() {
        return slug;
    }

    public CategoryType getType() {
        return type;
    }

    public boolean isPremium() {
        return premium;
    }

    public boolean isActive() {
        return active;
    }

    public Set<CategoryLocalization> getLocalizations() {
        return Collections.unmodifiableSet(localizations);
    }

    public Set<CategoryContent> getCuratedContents() {
        return Collections.unmodifiableSet(curatedContents);
    }

    public Optional<CategoryLocalization> findLocalization(LanguageCode languageCode) {
        LanguageCode requiredLanguageCode = requireLanguageCode(languageCode);
        return localizations.stream()
                .filter(candidate -> candidate.getLanguageCode() == requiredLanguageCode)
                .findFirst();
    }

    public boolean hasPublishedLocalization(LanguageCode languageCode) {
        return findLocalization(languageCode)
                .map(CategoryLocalization::isPublished)
                .orElse(false);
    }

    public Optional<CategoryContent> findCuratedContent(LanguageCode languageCode, Long contentId) {
        LanguageCode requiredLanguageCode = requireLanguageCode(languageCode);
        Long requiredContentId = requirePositiveId(contentId, "Curated content ID must be positive");
        return curatedContents.stream()
                .filter(candidate -> candidate.matchesLanguageAndContent(requiredLanguageCode, requiredContentId))
                .findFirst();
    }

    public void updateDetails(String slug, CategoryType type, boolean premium, boolean active) {
        this.slug = requireText(slug, "Category slug must not be blank");
        this.type = requireType(type);
        this.premium = premium;
        this.active = active;
    }

    /**
     * Creates or updates one localized category view.
     */
    public CategoryLocalization upsertLocalization(
            LanguageCode languageCode,
            String name,
            String description,
            Long imageMediaId,
            LocalizationStatus status,
            Instant publishedAt) {
        CategoryLocalization localization = findLocalization(languageCode)
                .orElseGet(() -> createLocalization(languageCode, name, status));
        localization.updateContent(name, description, imageMediaId);
        localization.markStatus(status, publishedAt);
        return localization;
    }

    /**
     * Adds curated content for one language after verifying publication state and order uniqueness.
     */
    public CategoryContent addContent(LanguageCode languageCode, Long contentId, int displayOrder) {
        requirePublishedLocalization(languageCode);
        if (findCuratedContent(languageCode, contentId).isPresent()) {
            throw new IllegalArgumentException("Curated content already exists for category and language");
        }
        requireDisplayOrderAvailability(languageCode, displayOrder, null);
        CategoryContent categoryContent = new CategoryContent(this, languageCode, contentId, displayOrder);
        curatedContents.add(categoryContent);
        return categoryContent;
    }

    /**
     * Updates the display order of a curated content link for one language.
     */
    public CategoryContent updateContentOrder(LanguageCode languageCode, Long contentId, int displayOrder) {
        requirePublishedLocalization(languageCode);
        CategoryContent curatedContent = findCuratedContent(languageCode, contentId)
                .orElseThrow(() -> new IllegalArgumentException("Curated content not found for category"));
        requireDisplayOrderAvailability(languageCode, displayOrder, contentId);
        curatedContent.updateDisplayOrder(displayOrder);
        return curatedContent;
    }

    public void removeContent(LanguageCode languageCode, Long contentId) {
        boolean removed = curatedContents.removeIf(candidate -> candidate.matchesLanguageAndContent(
                requireLanguageCode(languageCode),
                requirePositiveId(contentId, "Curated content ID must be positive")));
        if (!removed) {
            throw new IllegalArgumentException("Curated content not found for category");
        }
    }

    private CategoryLocalization createLocalization(
            LanguageCode languageCode,
            String name,
            LocalizationStatus status) {
        CategoryLocalization localization = new CategoryLocalization(this, languageCode, name, status);
        localizations.add(localization);
        return localization;
    }

    private void requirePublishedLocalization(LanguageCode languageCode) {
        if (!hasPublishedLocalization(languageCode)) {
            throw new IllegalStateException("Published category localization must exist before curation");
        }
    }

    private void requireDisplayOrderAvailability(LanguageCode languageCode, int displayOrder, Long currentContentId) {
        LanguageCode requiredLanguageCode = requireLanguageCode(languageCode);
        if (displayOrder < 0) {
            throw new IllegalArgumentException("Category display order must not be negative");
        }
        boolean alreadyUsed = curatedContents.stream()
                .filter(candidate -> candidate.matchesLanguage(requiredLanguageCode))
                .filter(candidate -> currentContentId == null || !candidate.getContentId().equals(currentContentId))
                .anyMatch(candidate -> candidate.getDisplayOrder() == displayOrder);
        if (alreadyUsed) {
            throw new IllegalArgumentException("Category display order must be unique per language");
        }
    }

    private static CategoryType requireType(CategoryType type) {
        if (type == null) {
            throw new IllegalArgumentException("Category type must not be null");
        }
        return type;
    }

    private static LanguageCode requireLanguageCode(LanguageCode languageCode) {
        if (languageCode == null) {
            throw new IllegalArgumentException("Language code must not be null");
        }
        return languageCode;
    }

    private static Long requirePositiveId(Long value, String message) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
