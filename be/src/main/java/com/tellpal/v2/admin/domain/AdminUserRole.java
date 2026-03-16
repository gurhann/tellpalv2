package com.tellpal.v2.admin.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "admin_user_roles")
public class AdminUserRole {

    @EmbeddedId
    private AdminUserRoleId id;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected AdminUserRole() {
    }

    public AdminUserRole(Long adminUserId, String roleCode) {
        this.id = new AdminUserRoleId(adminUserId, roleCode);
    }

    public AdminUserRoleId getId() {
        return id;
    }

    public Long getAdminUserId() {
        return id.getAdminUserId();
    }

    public String getRoleCode() {
        return id.getRoleCode();
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
