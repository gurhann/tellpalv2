package com.tellpal.v2.admin.api;

public record AdminRefreshCommand(String refreshToken, String userAgent, String ipAddress) {

    public AdminRefreshCommand {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Admin refresh token must not be blank");
        }
    }
}
