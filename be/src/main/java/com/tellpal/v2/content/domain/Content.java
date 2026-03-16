package com.tellpal.v2.content.domain;

import com.tellpal.v2.shared.infrastructure.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "v2_contents")
public class Content extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private ContentType type;

    @Column(name = "external_key", nullable = false, unique = true)
    private String externalKey;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "age_range")
    private String ageRange;

    @Column(name = "page_count")
    private Integer pageCount;

    protected Content() {
    }

    public Content(ContentType type, String externalKey) {
        this.type = type;
        this.externalKey = externalKey;
        this.isActive = true;
    }

    public ContentType getType() {
        return type;
    }

    public void setType(ContentType type) {
        this.type = type;
    }

    public String getExternalKey() {
        return externalKey;
    }

    public void setExternalKey(String externalKey) {
        this.externalKey = externalKey;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getAgeRange() {
        return ageRange;
    }

    public void setAgeRange(String ageRange) {
        this.ageRange = ageRange;
    }

    public Integer getPageCount() {
        return pageCount;
    }

    public void setPageCount(Integer pageCount) {
        this.pageCount = pageCount;
    }
}
