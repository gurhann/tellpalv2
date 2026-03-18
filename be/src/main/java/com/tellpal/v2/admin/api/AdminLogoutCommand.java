package com.tellpal.v2.admin.api;

/**
 * Logout request identified by the refresh token that backs the admin session.
 */
public record AdminLogoutCommand(String refreshToken) {

    public AdminLogoutCommand {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Admin refresh token must not be blank");
        }
    }
}
