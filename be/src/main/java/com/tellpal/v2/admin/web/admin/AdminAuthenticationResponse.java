package com.tellpal.v2.admin.web.admin;

import java.time.Instant;
import java.util.Set;

import com.tellpal.v2.admin.api.AdminAuthenticationResult;

public record AdminAuthenticationResponse(
        Long adminUserId,
        String username,
        Set<String> roleCodes,
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt) {

    static AdminAuthenticationResponse from(AdminAuthenticationResult result) {
        return new AdminAuthenticationResponse(
                result.adminUserId(),
                result.username(),
                result.roleCodes(),
                result.accessToken(),
                result.accessTokenExpiresAt(),
                result.refreshToken(),
                result.refreshTokenExpiresAt());
    }
}
