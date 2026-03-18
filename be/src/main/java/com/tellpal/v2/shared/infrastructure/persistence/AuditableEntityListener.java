package com.tellpal.v2.shared.infrastructure.persistence;

import java.time.Instant;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

/**
 * Persistence listener that keeps {@link BaseJpaEntity} audit timestamps in sync.
 */
public class AuditableEntityListener {

    /**
     * Initializes audit timestamps before first persistence.
     */
    @PrePersist
    public void prePersist(Object target) {
        if (target instanceof BaseJpaEntity entity) {
            entity.markCreated(Instant.now());
        }
    }

    /**
     * Refreshes the update timestamp before entity updates.
     */
    @PreUpdate
    public void preUpdate(Object target) {
        if (target instanceof BaseJpaEntity entity) {
            entity.markUpdated(Instant.now());
        }
    }
}
