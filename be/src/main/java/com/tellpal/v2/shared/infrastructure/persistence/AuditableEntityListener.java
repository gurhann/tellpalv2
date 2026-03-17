package com.tellpal.v2.shared.infrastructure.persistence;

import java.time.Instant;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

public class AuditableEntityListener {

    @PrePersist
    public void prePersist(Object target) {
        if (target instanceof BaseJpaEntity entity) {
            entity.markCreated(Instant.now());
        }
    }

    @PreUpdate
    public void preUpdate(Object target) {
        if (target instanceof BaseJpaEntity entity) {
            entity.markUpdated(Instant.now());
        }
    }
}
