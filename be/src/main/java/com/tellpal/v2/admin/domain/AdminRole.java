package com.tellpal.v2.admin.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import com.tellpal.v2.shared.infrastructure.persistence.BaseJpaEntity;

@Entity
@Table(name = "admin_roles")
public class AdminRole extends BaseJpaEntity {

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    protected AdminRole() {
    }

    private AdminRole(String code, String description) {
        this.code = requireText(code, "Role code must not be blank");
        this.description = requireText(description, "Role description must not be blank");
    }

    public static AdminRole create(String code, String description) {
        return new AdminRole(code, description);
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public void changeDescription(String description) {
        this.description = requireText(description, "Role description must not be blank");
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
