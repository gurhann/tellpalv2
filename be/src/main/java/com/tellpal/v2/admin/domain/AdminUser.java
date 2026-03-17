package com.tellpal.v2.admin.domain;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import com.tellpal.v2.shared.infrastructure.persistence.BaseJpaEntity;

@Entity
@Table(name = "admin_users")
public class AdminUser extends BaseJpaEntity {

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @OneToMany(mappedBy = "adminUser", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<AdminUserRole> roleAssignments = new LinkedHashSet<>();

    protected AdminUser() {
    }

    private AdminUser(String username, String passwordHash) {
        this.username = requireText(username, "Username must not be blank");
        this.passwordHash = requireText(passwordHash, "Password hash must not be blank");
        this.active = true;
    }

    public static AdminUser create(String username, String passwordHash) {
        return new AdminUser(username, passwordHash);
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public Set<AdminUserRole> getRoleAssignments() {
        return Collections.unmodifiableSet(roleAssignments);
    }

    public void updatePasswordHash(String passwordHash) {
        this.passwordHash = requireText(passwordHash, "Password hash must not be blank");
    }

    public void activate() {
        active = true;
    }

    public void deactivate() {
        active = false;
    }

    public void recordLogin(Instant loggedInAt) {
        if (loggedInAt == null) {
            throw new IllegalArgumentException("Login timestamp must not be null");
        }
        lastLoginAt = loggedInAt;
    }

    public void assignRole(AdminRole role, Instant assignedAt) {
        if (role == null) {
            throw new IllegalArgumentException("Role must not be null");
        }
        if (roleAssignments.stream().anyMatch(assignment -> assignment.hasRoleCode(role.getCode()))) {
            return;
        }
        roleAssignments.add(new AdminUserRole(this, role, assignedAt));
    }

    public void revokeRole(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            throw new IllegalArgumentException("Role code must not be blank");
        }
        roleAssignments.removeIf(assignment -> assignment.hasRoleCode(roleCode));
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
