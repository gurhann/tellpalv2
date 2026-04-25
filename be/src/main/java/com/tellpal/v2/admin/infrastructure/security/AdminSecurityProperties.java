package com.tellpal.v2.admin.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("tellpal.security.admin")
public record AdminSecurityProperties(
        String jwtIssuer,
        String jwtSecret,
        Duration accessTokenTtl,
        Duration refreshTokenTtl,
        int bcryptStrength,
        BruteForceProperties bruteForce,
        CorsProperties cors) {

    private static final int MIN_JWT_SECRET_BYTES = 32;

    public AdminSecurityProperties {
        if (jwtIssuer == null || jwtIssuer.isBlank()) {
            throw new IllegalArgumentException("Admin JWT issuer must not be blank");
        }
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalArgumentException("Admin JWT secret must not be blank");
        }
        if (accessTokenTtl == null || accessTokenTtl.isZero() || accessTokenTtl.isNegative()) {
            throw new IllegalArgumentException("Admin access token TTL must be positive");
        }
        if (refreshTokenTtl == null || refreshTokenTtl.isZero() || refreshTokenTtl.isNegative()) {
            throw new IllegalArgumentException("Admin refresh token TTL must be positive");
        }
        if (bcryptStrength < 10 || bcryptStrength > 31) {
            throw new IllegalArgumentException("Admin BCrypt strength must be between 10 and 31");
        }
        bruteForce = bruteForce == null ? BruteForceProperties.defaults() : bruteForce;
        cors = cors == null ? new CorsProperties(List.of()) : cors;
    }

    public SecretKey jwtSecretKey() {
        byte[] secretBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MIN_JWT_SECRET_BYTES) {
            throw new IllegalArgumentException("Admin JWT secret must be at least 32 bytes");
        }
        return new SecretKeySpec(secretBytes, "HmacSHA256");
    }

    public record CorsProperties(List<String> allowedOrigins) {

        public CorsProperties {
            allowedOrigins = allowedOrigins == null
                    ? List.of()
                    : allowedOrigins.stream()
                            .filter(origin -> origin != null && !origin.isBlank())
                            .map(String::trim)
                            .toList();
        }
    }

    public record BruteForceProperties(
            boolean enabled,
            Duration window,
            Duration lockoutDuration,
            int maxLoginFailuresPerUsername,
            int maxLoginFailuresPerIp,
            int maxRefreshFailuresPerIp,
            int maxEntries) {

        private static final Duration DEFAULT_WINDOW = Duration.ofMinutes(15);
        private static final Duration DEFAULT_LOCKOUT_DURATION = Duration.ofMinutes(15);

        public BruteForceProperties {
            if (window == null || window.isZero() || window.isNegative()) {
                throw new IllegalArgumentException("Admin brute-force window must be positive");
            }
            if (lockoutDuration == null || lockoutDuration.isZero() || lockoutDuration.isNegative()) {
                throw new IllegalArgumentException("Admin brute-force lockout duration must be positive");
            }
            if (maxLoginFailuresPerUsername < 1) {
                throw new IllegalArgumentException("Admin login username failure limit must be positive");
            }
            if (maxLoginFailuresPerIp < 1) {
                throw new IllegalArgumentException("Admin login IP failure limit must be positive");
            }
            if (maxRefreshFailuresPerIp < 1) {
                throw new IllegalArgumentException("Admin refresh IP failure limit must be positive");
            }
            if (maxEntries < 1) {
                throw new IllegalArgumentException("Admin brute-force max entries must be positive");
            }
        }

        static BruteForceProperties defaults() {
            return new BruteForceProperties(true, DEFAULT_WINDOW, DEFAULT_LOCKOUT_DURATION, 5, 20, 30, 10_000);
        }
    }
}
