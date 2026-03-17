package com.tellpal.v2.category.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.tellpal.v2.shared.domain.LanguageCode;
import com.tellpal.v2.shared.infrastructure.persistence.BaseJpaEntity;

@Entity
@Table(name = "category_localizations")
public class CategoryLocalization extends BaseJpaEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "language_code", nullable = false, length = 8)
    private LanguageCode languageCode;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "image_media_id")
    private Long imageMediaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LocalizationStatus status;

    @Column(name = "published_at")
    private Instant publishedAt;

    protected CategoryLocalization() {
    }

    CategoryLocalization(
            Category category,
            LanguageCode languageCode,
            String name,
            LocalizationStatus status) {
        this.category = requireCategory(category);
        this.languageCode = requireLanguageCode(languageCode);
        this.name = requireText(name, "Category localization name must not be blank");
        this.status = requireStatus(status);
    }

    public LanguageCode getLanguageCode() {
        return languageCode;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Long getImageMediaId() {
        return imageMediaId;
    }

    public LocalizationStatus getStatus() {
        return status;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public boolean isPublished() {
        return status == LocalizationStatus.PUBLISHED;
    }

    public void updateContent(String name, String description, Long imageMediaId) {
        this.name = requireText(name, "Category localization name must not be blank");
        this.description = normalizeOptionalText(description);
        this.imageMediaId = normalizePositiveId(imageMediaId, "Category image media ID must be positive");
    }

    public void markStatus(LocalizationStatus status, Instant publishedAt) {
        LocalizationStatus nextStatus = requireStatus(status);
        if (nextStatus == LocalizationStatus.PUBLISHED && publishedAt == null) {
            throw new IllegalArgumentException("Published category localization must include a publish timestamp");
        }
        this.status = nextStatus;
        this.publishedAt = nextStatus == LocalizationStatus.PUBLISHED ? publishedAt : this.publishedAt;
    }

    private static Category requireCategory(Category category) {
        if (category == null) {
            throw new IllegalArgumentException("Category must not be null");
        }
        return category;
    }

    private static LanguageCode requireLanguageCode(LanguageCode languageCode) {
        if (languageCode == null) {
            throw new IllegalArgumentException("Language code must not be null");
        }
        return languageCode;
    }

    private static LocalizationStatus requireStatus(LocalizationStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Category localization status must not be null");
        }
        return status;
    }

    private static String requireText(String value, String message) {
        String normalized = normalizeOptionalText(value);
        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private static String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
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
