package com.tellpal.v2.admin.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class AdminUserRoleId implements Serializable {

    @Column(name = "admin_user_id", nullable = false)
    private Long adminUserId;

    @Column(name = "role_code", nullable = false)
    private String roleCode;

    protected AdminUserRoleId() {
    }

    public AdminUserRoleId(Long adminUserId, String roleCode) {
        this.adminUserId = adminUserId;
        this.roleCode = roleCode;
    }

    public Long getAdminUserId() {
        return adminUserId;
    }

    public String getRoleCode() {
        return roleCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AdminUserRoleId that)) return false;
        return Objects.equals(adminUserId, that.adminUserId) &&
               Objects.equals(roleCode, that.roleCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(adminUserId, roleCode);
    }
}
