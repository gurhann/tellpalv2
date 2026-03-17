package com.tellpal.v2.admin.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("tellpal.security.admin")
public record AdminSecurityProperties(
        String jwtIssuer,
        String jwtSecret,
        Duration accessTokenTtl,
        Duration refreshTokenTtl,
        int bcryptStrength) {

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
    }

    public SecretKey jwtSecretKey() {
        byte[] secretBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MIN_JWT_SECRET_BYTES) {
            throw new IllegalArgumentException("Admin JWT secret must be at least 32 bytes");
        }
        return new SecretKeySpec(secretBytes, "HmacSHA256");
    }
}
