package com.tellpal.v2.shared.infrastructure.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
@EntityListeners(AuditableEntityListener.class)
public abstract class BaseJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BaseJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    void markCreated(Instant now) {
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    void markUpdated(Instant now) {
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }
}
