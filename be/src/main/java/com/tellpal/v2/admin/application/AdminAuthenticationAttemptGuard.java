package com.tellpal.v2.admin.application;

/**
 * Guards admin authentication attempts against repeated failed login and refresh attempts.
 */
public interface AdminAuthenticationAttemptGuard {

    void assertLoginAllowed(String username, String ipAddress);

    void recordLoginFailure(String username, String ipAddress);

    void clearLoginFailures(String username);

    void assertRefreshAllowed(String refreshTokenHash, String ipAddress);

    void recordRefreshFailure(String refreshTokenHash, String ipAddress);

    void clearRefreshFailures(String refreshTokenHash);
}
