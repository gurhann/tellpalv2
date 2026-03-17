package com.tellpal.v2.admin.api;

public record AdminLogoutCommand(String refreshToken) {

    public AdminLogoutCommand {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Admin refresh token must not be blank");
        }
    }
}
