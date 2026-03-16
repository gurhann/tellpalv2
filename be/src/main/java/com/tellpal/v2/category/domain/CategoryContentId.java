package com.tellpal.v2.category.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class CategoryContentId implements Serializable {

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "language_code", nullable = false, length = 10)
    private String languageCode;

    @Column(name = "content_id", nullable = false)
    private Long contentId;

    protected CategoryContentId() {
    }

    public CategoryContentId(Long categoryId, String languageCode, Long contentId) {
        this.categoryId = categoryId;
        this.languageCode = languageCode;
        this.contentId = contentId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public Long getContentId() {
        return contentId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CategoryContentId that)) return false;
        return Objects.equals(categoryId, that.categoryId)
            && Objects.equals(languageCode, that.languageCode)
            && Objects.equals(contentId, that.contentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(categoryId, languageCode, contentId);
    }
}
