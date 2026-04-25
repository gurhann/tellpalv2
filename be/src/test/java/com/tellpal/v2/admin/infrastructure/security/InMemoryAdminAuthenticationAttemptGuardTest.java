package com.tellpal.v2.admin.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.tellpal.v2.admin.application.AdminAuthenticationRateLimitExceededException;

class InMemoryAdminAuthenticationAttemptGuardTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-03-17T10:00:00Z");

    @Test
    void blocksUsernameAfterFiveFailedLoginAttempts() {
        InMemoryAdminAuthenticationAttemptGuard guard = guard(5, 20, 30);

        for (int attempt = 0; attempt < 5; attempt++) {
            guard.assertLoginAllowed("Admin-Root", "203.0.113.10");
            guard.recordLoginFailure("Admin-Root", "203.0.113.10");
        }

        assertThatThrownBy(() -> guard.assertLoginAllowed("admin-root", "203.0.113.11"))
                .isInstanceOf(AdminAuthenticationRateLimitExceededException.class);
    }

    @Test
    void successfulLoginClearsUsernameFailures() {
        InMemoryAdminAuthenticationAttemptGuard guard = guard(5, 20, 30);

        for (int attempt = 0; attempt < 4; attempt++) {
            guard.recordLoginFailure("admin-root", "203.0.113.10");
        }

        guard.clearLoginFailures("admin-root");

        for (int attempt = 0; attempt < 4; attempt++) {
            guard.assertLoginAllowed("admin-root", "203.0.113.11");
            guard.recordLoginFailure("admin-root", "203.0.113.11");
        }
        guard.assertLoginAllowed("admin-root", "203.0.113.12");
    }

    @Test
    void blocksDifferentUsernamesAfterIpLoginThreshold() {
        InMemoryAdminAuthenticationAttemptGuard guard = guard(5, 2, 30);

        guard.recordLoginFailure("first-admin", "203.0.113.10");
        guard.recordLoginFailure("second-admin", "203.0.113.10");

        assertThatThrownBy(() -> guard.assertLoginAllowed("third-admin", "203.0.113.10"))
                .isInstanceOf(AdminAuthenticationRateLimitExceededException.class);
    }

    @Test
    void refreshFailuresAreTrackedByTokenHashAndIp() {
        InMemoryAdminAuthenticationAttemptGuard guard = guard(5, 20, 2);

        guard.recordRefreshFailure("refresh-token-hash", "203.0.113.10");
        guard.recordRefreshFailure("refresh-token-hash", "203.0.113.11");

        assertThatThrownBy(() -> guard.assertRefreshAllowed("refresh-token-hash", "203.0.113.12"))
                .isInstanceOf(AdminAuthenticationRateLimitExceededException.class);

        InMemoryAdminAuthenticationAttemptGuard ipGuard = guard(5, 20, 2);
        ipGuard.recordRefreshFailure("first-refresh-token-hash", "203.0.113.20");
        ipGuard.recordRefreshFailure("second-refresh-token-hash", "203.0.113.20");

        assertThatThrownBy(() -> ipGuard.assertRefreshAllowed("third-refresh-token-hash", "203.0.113.20"))
                .isInstanceOf(AdminAuthenticationRateLimitExceededException.class);
    }

    @Test
    void successfulRefreshClearsTokenHashFailures() {
        InMemoryAdminAuthenticationAttemptGuard guard = guard(5, 20, 2);

        guard.recordRefreshFailure("refresh-token-hash", "203.0.113.10");
        guard.clearRefreshFailures("refresh-token-hash");

        guard.assertRefreshAllowed("refresh-token-hash", "203.0.113.11");
    }

    private static InMemoryAdminAuthenticationAttemptGuard guard(
            int maxLoginFailuresPerUsername,
            int maxLoginFailuresPerIp,
            int maxRefreshFailuresPerIp) {
        return new InMemoryAdminAuthenticationAttemptGuard(
                Clock.fixed(FIXED_NOW, ZoneOffset.UTC),
                new AdminSecurityProperties(
                        "tellpal-v2-admin",
                        "change-me-change-me-change-me-32-bytes",
                        Duration.ofHours(1),
                        Duration.ofDays(30),
                        10,
                        new AdminSecurityProperties.BruteForceProperties(
                                true,
                                Duration.ofMinutes(15),
                                Duration.ofMinutes(15),
                                maxLoginFailuresPerUsername,
                                maxLoginFailuresPerIp,
                                maxRefreshFailuresPerIp,
                                10_000),
                        new AdminSecurityProperties.CorsProperties(java.util.List.of())));
    }
}
