package com.tellpal.v2.admin.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.tellpal.v2.shared.infrastructure.persistence.BaseJpaEntity;

@Entity
@Table(name = "admin_refresh_tokens")
public class AdminRefreshToken extends BaseJpaEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_user_id", nullable = false)
    private AdminUser adminUser;

    @Column(name = "refresh_token_hash", nullable = false, length = 128)
    private String refreshTokenHash;

    @Column(name = "replaced_by_token_hash", length = 128)
    private String replacedByTokenHash;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    protected AdminRefreshToken() {
    }

    private AdminRefreshToken(
            AdminUser adminUser,
            String refreshTokenHash,
            Instant issuedAt,
            Instant expiresAt,
            String userAgent,
            String ipAddress) {
        if (adminUser == null) {
            throw new IllegalArgumentException("Admin user must not be null");
        }
        if (issuedAt == null || expiresAt == null) {
            throw new IllegalArgumentException("Token timestamps must not be null");
        }
        if (!expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException("Token expiry must be after issue time");
        }
        this.adminUser = adminUser;
        this.refreshTokenHash = requireText(refreshTokenHash, "Refresh token hash must not be blank");
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.userAgent = userAgent;
        this.ipAddress = ipAddress;
    }

    public static AdminRefreshToken issue(
            AdminUser adminUser,
            String refreshTokenHash,
            Instant issuedAt,
            Instant expiresAt,
            String userAgent,
            String ipAddress) {
        return new AdminRefreshToken(adminUser, refreshTokenHash, issuedAt, expiresAt, userAgent, ipAddress);
    }

    public String getRefreshTokenHash() {
        return refreshTokenHash;
    }

    public AdminUser getAdminUser() {
        return adminUser;
    }

    public String getReplacedByTokenHash() {
        return replacedByTokenHash;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public boolean isExpiredAt(Instant instant) {
        if (instant == null) {
            throw new IllegalArgumentException("Instant must not be null");
        }
        return !expiresAt.isAfter(instant);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public void markRotatedTo(String replacedByTokenHash) {
        this.replacedByTokenHash = requireText(replacedByTokenHash, "Replacement token hash must not be blank");
    }

    public void revoke(Instant revokedAt) {
        if (revokedAt == null) {
            throw new IllegalArgumentException("Revocation timestamp must not be null");
        }
        this.revokedAt = revokedAt;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
