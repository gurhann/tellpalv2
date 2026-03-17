package com.tellpal.v2.content.domain;

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
@Table(name = "content_contributors")
public class ContentContributor extends BaseJpaEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "content_id", nullable = false)
    private Content content;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contributor_id", nullable = false)
    private Contributor contributor;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private ContributorRole role;

    @Column(name = "language_code", length = 8)
    private LanguageCode languageCode;

    @Column(name = "credit_name", length = 200)
    private String creditName;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected ContentContributor() {
    }

    ContentContributor(
            Content content,
            Contributor contributor,
            ContributorRole role,
            LanguageCode languageCode,
            String creditName,
            int sortOrder) {
        this.content = requireContent(content);
        this.contributor = requireContributor(contributor);
        this.role = requireRole(role);
        this.languageCode = languageCode;
        this.creditName = normalizeOptionalText(creditName);
        this.sortOrder = requireNonNegative(sortOrder);
    }

    public Contributor getContributor() {
        return contributor;
    }

    public ContributorRole getRole() {
        return role;
    }

    public LanguageCode getLanguageCode() {
        return languageCode;
    }

    public String getCreditName() {
        return creditName;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public boolean matchesRoleAndLanguage(ContributorRole role, LanguageCode languageCode) {
        return this.role == role && this.languageCode == languageCode;
    }

    public boolean matchesAssignment(Long contributorId, ContributorRole role, LanguageCode languageCode) {
        return hasContributor(contributorId) && matchesRoleAndLanguage(role, languageCode);
    }

    public boolean hasContributor(Long contributorId) {
        Long id = contributor.getId();
        return id != null && id.equals(contributorId);
    }

    public void updateCredit(String creditName, int sortOrder) {
        this.creditName = normalizeOptionalText(creditName);
        this.sortOrder = requireNonNegative(sortOrder);
    }

    private static Content requireContent(Content content) {
        if (content == null) {
            throw new IllegalArgumentException("Content must not be null");
        }
        return content;
    }

    private static Contributor requireContributor(Contributor contributor) {
        if (contributor == null) {
            throw new IllegalArgumentException("Contributor must not be null");
        }
        return contributor;
    }

    private static ContributorRole requireRole(ContributorRole role) {
        if (role == null) {
            throw new IllegalArgumentException("Contributor role must not be null");
        }
        return role;
    }

    private static String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static int requireNonNegative(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Contributor sort order must not be negative");
        }
        return value;
    }
}
