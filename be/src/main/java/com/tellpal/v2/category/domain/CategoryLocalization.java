package com.tellpal.v2.category.domain;

import com.tellpal.v2.shared.domain.LocalizationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "v2_category_localizations")
public class CategoryLocalization {

    @EmbeddedId
    private CategoryLocalizationId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("categoryId")
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "image_media_id")
    private Long imageMediaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private LocalizationStatus status = LocalizationStatus.DRAFT;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected CategoryLocalization() {
    }

    public CategoryLocalization(Category category, String languageCode, String name) {
        this.category = category;
        this.id = new CategoryLocalizationId(category.getId(), languageCode);
        this.name = name;
        this.status = LocalizationStatus.DRAFT;
    }

    public CategoryLocalizationId getId() {
        return id;
    }

    public Category getCategory() {
        return category;
    }

    public String getLanguageCode() {
        return id.getLanguageCode();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getImageMediaId() {
        return imageMediaId;
    }

    public void setImageMediaId(Long imageMediaId) {
        this.imageMediaId = imageMediaId;
    }

    public LocalizationStatus getStatus() {
        return status;
    }

    public void setStatus(LocalizationStatus status) {
        this.status = status;
    }

    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(OffsetDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
