package com.tellpal.v2.content.domain;

import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import com.tellpal.v2.shared.infrastructure.persistence.BaseJpaEntity;

/**
 * Aggregate root for contributor identity shared across content assignments.
 */
@Entity
@Table(name = "contributors")
public class Contributor extends BaseJpaEntity {

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Column(name = "normalized_display_name", insertable = false, updatable = false, length = 200)
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    private String normalizedDisplayName;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "contributor_roles", joinColumns = @JoinColumn(name = "contributor_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Set<ContributorRole> roles = new LinkedHashSet<>();

    protected Contributor() {
    }

    private Contributor(String displayName, Set<ContributorRole> roles) {
        this.displayName = requireText(displayName, "Contributor display name must not be blank");
        this.roles = requireRoles(roles);
    }

    /**
     * Creates a contributor with a stable display name.
     */
    public static Contributor create(String displayName) {
        return new Contributor(displayName, Set.of(ContributorRole.AUTHOR));
    }

    /**
     * Creates a contributor with one or more discovery roles.
     */
    public static Contributor create(String displayName, Set<ContributorRole> roles) {
        return new Contributor(displayName, roles);
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getNormalizedDisplayName() {
        return normalizedDisplayName;
    }

    /**
     * Returns a read-only snapshot so callers cannot bypass the role invariant.
     */
    public Set<ContributorRole> getRoles() {
        return Set.copyOf(roles);
    }

    /**
     * Renames the contributor.
     */
    public void rename(String displayName) {
        this.displayName = requireText(displayName, "Contributor display name must not be blank");
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static Set<ContributorRole> requireRoles(Set<ContributorRole> roles) {
        if (roles == null || roles.isEmpty() || roles.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("Contributor must have at least one role");
        }
        return new LinkedHashSet<>(roles);
    }
}
