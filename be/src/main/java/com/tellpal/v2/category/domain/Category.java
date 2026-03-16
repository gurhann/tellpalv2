package com.tellpal.v2.category.domain;

import com.tellpal.v2.shared.infrastructure.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "v2_categories")
public class Category extends BaseEntity {

    @Column(name = "slug", nullable = false, unique = true)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private CategoryType type;

    @Column(name = "is_premium", nullable = false)
    private boolean isPremium = false;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    protected Category() {
    }

    public Category(String slug, CategoryType type) {
        this.slug = slug;
        this.type = type;
        this.isPremium = false;
        this.isActive = true;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public CategoryType getType() {
        return type;
    }

    public void setType(CategoryType type) {
        this.type = type;
    }

    public boolean isPremium() {
        return isPremium;
    }

    public void setPremium(boolean premium) {
        isPremium = premium;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
