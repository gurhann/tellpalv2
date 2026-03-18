package com.tellpal.v2.admin.api;

/**
 * Login request for an admin user.
 *
 * <p>User agent and IP address are optional request metadata captured for refresh token auditing.
 */
public record AdminLoginCommand(String username, String password, String userAgent, String ipAddress) {

    public AdminLoginCommand {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Admin username must not be blank");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Admin password must not be blank");
        }
    }
}
