package com.tellpal.v2.admin.infrastructure.security;

import java.util.Set;

public record AdminAccessTokenSubject(Long adminUserId, String username, Set<String> roleCodes) {

    public AdminAccessTokenSubject {
        if (adminUserId == null || adminUserId <= 0) {
            throw new IllegalArgumentException("Admin user ID must be positive");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Admin username must not be blank");
        }
        if (roleCodes == null) {
            throw new IllegalArgumentException("Admin role codes must not be null");
        }
        roleCodes = roleCodes.stream()
                .map(AdminAccessTokenSubject::requireRoleCode)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static String requireRoleCode(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            throw new IllegalArgumentException("Admin role code must not be blank");
        }
        return roleCode;
    }
}
