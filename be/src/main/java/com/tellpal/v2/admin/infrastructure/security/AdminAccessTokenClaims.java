package com.tellpal.v2.admin.infrastructure.security;

import java.time.Instant;
import java.util.Set;

public record AdminAccessTokenClaims(
        Long adminUserId,
        String username,
        Set<String> roleCodes,
        Instant issuedAt,
        Instant expiresAt) {

    public AdminAccessTokenClaims {
        if (adminUserId == null || adminUserId <= 0) {
            throw new IllegalArgumentException("Admin user ID must be positive");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Admin username must not be blank");
        }
        if (roleCodes == null) {
            throw new IllegalArgumentException("Admin role codes must not be null");
        }
        if (issuedAt == null || expiresAt == null) {
            throw new IllegalArgumentException("Admin token timestamps must not be null");
        }
        if (!expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException("Admin token expiry must be after issue time");
        }
        roleCodes = roleCodes.stream()
                .map(AdminAccessTokenClaims::requireRoleCode)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static String requireRoleCode(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            throw new IllegalArgumentException("Admin role code must not be blank");
        }
        return roleCode;
    }
}
