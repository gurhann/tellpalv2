package com.tellpal.v2.shared.web.admin;

import java.util.Set;

/**
 * Normalized authenticated admin principal exposed to admin controllers and error handlers.
 */
public record AuthenticatedAdmin(Long adminUserId, String username, Set<String> roleCodes) {

    public AuthenticatedAdmin {
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
                .map(AuthenticatedAdmin::normalizeRoleCode)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static String normalizeRoleCode(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            throw new IllegalArgumentException("Admin role code must not be blank");
        }
        return roleCode.startsWith("ROLE_") ? roleCode.substring(5) : roleCode;
    }
}
