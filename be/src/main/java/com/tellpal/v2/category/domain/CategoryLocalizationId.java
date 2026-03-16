package com.tellpal.v2.category.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class CategoryLocalizationId implements Serializable {

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "language_code", nullable = false, length = 10)
    private String languageCode;

    protected CategoryLocalizationId() {
    }

    public CategoryLocalizationId(Long categoryId, String languageCode) {
        this.categoryId = categoryId;
        this.languageCode = languageCode;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CategoryLocalizationId that)) return false;
        return Objects.equals(categoryId, that.categoryId)
            && Objects.equals(languageCode, that.languageCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(categoryId, languageCode);
    }
}
