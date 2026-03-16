package com.tellpal.v2.category.domain;

import com.tellpal.v2.shared.infrastructure.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "v2_category_contents")
public class CategoryContent extends BaseEntity {

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "language_code", nullable = false, length = 10)
    private String languageCode;

    @Column(name = "content_id", nullable = false)
    private Long contentId;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    protected CategoryContent() {
    }

    public CategoryContent(Long categoryId, String languageCode, Long contentId, int displayOrder) {
        this.categoryId = categoryId;
        this.languageCode = languageCode;
        this.contentId = contentId;
        this.displayOrder = displayOrder;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    public Long getContentId() {
        return contentId;
    }

    public void setContentId(Long contentId) {
        this.contentId = contentId;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }
}
