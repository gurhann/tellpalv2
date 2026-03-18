package com.tellpal.v2.admin.api;

/**
 * Refresh request for rotating an existing admin refresh token.
 *
 * <p>User agent and IP address are optional request metadata stored with the replacement token.
 */
public record AdminRefreshCommand(String refreshToken, String userAgent, String ipAddress) {

    public AdminRefreshCommand {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Admin refresh token must not be blank");
        }
    }
}
