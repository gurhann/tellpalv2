package com.tellpal.v2.content.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import com.tellpal.v2.shared.infrastructure.persistence.BaseJpaEntity;

/**
 * Aggregate root for contributor identity shared across content assignments.
 */
@Entity
@Table(name = "contributors")
public class Contributor extends BaseJpaEntity {

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    protected Contributor() {
    }

    private Contributor(String displayName) {
        this.displayName = requireText(displayName, "Contributor display name must not be blank");
    }

    /**
     * Creates a contributor with a stable display name.
     */
    public static Contributor create(String displayName) {
        return new Contributor(displayName);
    }

    public String getDisplayName() {
        return displayName;
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
}
