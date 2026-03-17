package com.tellpal.v2.admin.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "admin_user_roles")
public class AdminUserRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_user_id", nullable = false)
    private AdminUser adminUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_role_id", nullable = false)
    private AdminRole adminRole;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    protected AdminUserRole() {
    }

    AdminUserRole(AdminUser adminUser, AdminRole adminRole, Instant assignedAt) {
        if (adminUser == null) {
            throw new IllegalArgumentException("Admin user must not be null");
        }
        if (adminRole == null) {
            throw new IllegalArgumentException("Admin role must not be null");
        }
        if (assignedAt == null) {
            throw new IllegalArgumentException("Assignment timestamp must not be null");
        }
        this.adminUser = adminUser;
        this.adminRole = adminRole;
        this.assignedAt = assignedAt;
    }

    public Long getId() {
        return id;
    }

    public AdminRole getAdminRole() {
        return adminRole;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    boolean hasRoleCode(String roleCode) {
        return adminRole.getCode().equals(roleCode);
    }
}
