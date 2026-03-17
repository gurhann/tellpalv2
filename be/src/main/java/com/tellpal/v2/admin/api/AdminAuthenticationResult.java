package com.tellpal.v2.admin.api;

import java.time.Instant;
import java.util.Set;

public record AdminAuthenticationResult(
        Long adminUserId,
        String username,
        Set<String> roleCodes,
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt) {

    public AdminAuthenticationResult {
        if (adminUserId == null || adminUserId <= 0) {
            throw new IllegalArgumentException("Admin user ID must be positive");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Admin username must not be blank");
        }
        if (roleCodes == null) {
            throw new IllegalArgumentException("Admin role codes must not be null");
        }
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("Admin access token must not be blank");
        }
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Admin refresh token must not be blank");
        }
        if (accessTokenExpiresAt == null || refreshTokenExpiresAt == null) {
            throw new IllegalArgumentException("Admin token expiry timestamps must not be null");
        }
        roleCodes = roleCodes.stream()
                .map(AdminAuthenticationResult::requireRoleCode)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static String requireRoleCode(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            throw new IllegalArgumentException("Admin role code must not be blank");
        }
        return roleCode;
    }
}
