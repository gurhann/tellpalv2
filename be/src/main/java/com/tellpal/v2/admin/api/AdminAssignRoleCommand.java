package com.tellpal.v2.admin.api;

public record AdminAssignRoleCommand(Long adminUserId, String roleCode) {

    public AdminAssignRoleCommand {
        if (adminUserId == null || adminUserId <= 0) {
            throw new IllegalArgumentException("Admin user ID must be positive");
        }
        if (roleCode == null || roleCode.isBlank()) {
            throw new IllegalArgumentException("Admin role code must not be blank");
        }
    }
}
