package com.tellpal.v2.content.domain;

import com.tellpal.v2.shared.infrastructure.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "v2_content_contributors")
public class ContentContributor extends BaseEntity {

    @Column(name = "content_id", nullable = false)
    private Long contentId;

    @Column(name = "contributor_id", nullable = false)
    private Long contributorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    private ContributorRole role;

    @Column(name = "language_code", length = 10)
    private String languageCode;

    @Column(name = "credit_name")
    private String creditName;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    protected ContentContributor() {
    }

    public ContentContributor(Long contentId, Long contributorId, ContributorRole role) {
        this.contentId = contentId;
        this.contributorId = contributorId;
        this.role = role;
        this.sortOrder = 0;
    }

    public Long getContentId() {
        return contentId;
    }

    public Long getContributorId() {
        return contributorId;
    }

    public ContributorRole getRole() {
        return role;
    }

    public void setRole(ContributorRole role) {
        this.role = role;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    public String getCreditName() {
        return creditName;
    }

    public void setCreditName(String creditName) {
        this.creditName = creditName;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
