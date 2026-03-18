package com.tellpal.v2.category.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.tellpal.v2.shared.domain.LanguageCode;
import com.tellpal.v2.shared.infrastructure.persistence.BaseJpaEntity;

/**
 * Category-owned curated content link scoped by language and display order.
 */
@Entity
@Table(name = "category_contents")
public class CategoryContent extends BaseJpaEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "language_code", nullable = false, length = 8)
    private LanguageCode languageCode;

    @Column(name = "content_id", nullable = false)
    private Long contentId;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    protected CategoryContent() {
    }

    CategoryContent(Category category, LanguageCode languageCode, Long contentId, int displayOrder) {
        this.category = requireCategory(category);
        this.languageCode = requireLanguageCode(languageCode);
        this.contentId = requirePositiveId(contentId, "Curated content ID must be positive");
        this.displayOrder = requireNonNegative(displayOrder);
    }

    public LanguageCode getLanguageCode() {
        return languageCode;
    }

    public Long getContentId() {
        return contentId;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public boolean matchesLanguage(LanguageCode languageCode) {
        return this.languageCode == languageCode;
    }

    public boolean matchesLanguageAndContent(LanguageCode languageCode, Long contentId) {
        return matchesLanguage(languageCode) && this.contentId.equals(contentId);
    }

    public void updateDisplayOrder(int displayOrder) {
        this.displayOrder = requireNonNegative(displayOrder);
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

    private static Long requirePositiveId(Long value, String message) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static int requireNonNegative(int displayOrder) {
        if (displayOrder < 0) {
            throw new IllegalArgumentException("Category display order must not be negative");
        }
        return displayOrder;
    }
}
