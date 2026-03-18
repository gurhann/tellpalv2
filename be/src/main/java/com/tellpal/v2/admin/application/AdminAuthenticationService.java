package com.tellpal.v2.admin.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tellpal.v2.admin.api.AdminAuthenticationApi;
import com.tellpal.v2.admin.api.AdminAuthenticationResult;
import com.tellpal.v2.admin.api.AdminLoginCommand;
import com.tellpal.v2.admin.api.AdminLogoutCommand;
import com.tellpal.v2.admin.api.AdminRefreshCommand;
import com.tellpal.v2.admin.domain.AdminRefreshToken;
import com.tellpal.v2.admin.domain.AdminRefreshTokenRepository;
import com.tellpal.v2.admin.domain.AdminUser;
import com.tellpal.v2.admin.domain.AdminUserRepository;
import com.tellpal.v2.admin.infrastructure.security.AdminAccessTokenSubject;
import com.tellpal.v2.admin.infrastructure.security.AdminJwtService;
import com.tellpal.v2.admin.infrastructure.security.AdminPasswordHasher;
import com.tellpal.v2.admin.infrastructure.security.AdminRefreshTokenGenerator;
import com.tellpal.v2.admin.infrastructure.security.AdminRefreshTokenHasher;
import com.tellpal.v2.admin.infrastructure.security.AdminSecurityProperties;
import com.tellpal.v2.admin.infrastructure.security.IssuedAdminAccessToken;

/**
 * Application service that orchestrates admin authentication, refresh token rotation, and logout.
 *
 * <p>This service coordinates admin aggregates, refresh token persistence, password verification,
 * and JWT issuance inside transactional use cases so that token state changes stay consistent with
 * the stored session model.
 */
@Service
public class AdminAuthenticationService implements AdminAuthenticationApi {

    private static final int REFRESH_TOKEN_GENERATION_ATTEMPTS = 5;

    private final Clock clock;
    private final AdminUserRepository adminUserRepository;
    private final AdminRefreshTokenRepository adminRefreshTokenRepository;
    private final AdminPasswordHasher adminPasswordHasher;
    private final AdminRefreshTokenHasher adminRefreshTokenHasher;
    private final AdminRefreshTokenGenerator adminRefreshTokenGenerator;
    private final AdminJwtService adminJwtService;
    private final AdminSecurityProperties adminSecurityProperties;

    public AdminAuthenticationService(
            Clock clock,
            AdminUserRepository adminUserRepository,
            AdminRefreshTokenRepository adminRefreshTokenRepository,
            AdminPasswordHasher adminPasswordHasher,
            AdminRefreshTokenHasher adminRefreshTokenHasher,
            AdminRefreshTokenGenerator adminRefreshTokenGenerator,
            AdminJwtService adminJwtService,
            AdminSecurityProperties adminSecurityProperties) {
        this.clock = clock;
        this.adminUserRepository = adminUserRepository;
        this.adminRefreshTokenRepository = adminRefreshTokenRepository;
        this.adminPasswordHasher = adminPasswordHasher;
        this.adminRefreshTokenHasher = adminRefreshTokenHasher;
        this.adminRefreshTokenGenerator = adminRefreshTokenGenerator;
        this.adminJwtService = adminJwtService;
        this.adminSecurityProperties = adminSecurityProperties;
    }

    /**
     * Verifies admin credentials, records the login timestamp, and opens a new authenticated
     * session.
     *
     * <p>Disabled users and invalid credentials fail before any token is issued.
     */
    @Override
    @Transactional
    public AdminAuthenticationResult login(AdminLoginCommand command) {
        AdminUser adminUser = adminUserRepository.findByUsername(command.username())
                .orElseThrow(() -> new AdminAuthenticationFailedException("Invalid admin credentials"));
        if (!adminUser.isActive()) {
            throw new AdminUserDisabledException(requireAdminUserId(adminUser));
        }
        if (!adminPasswordHasher.matches(command.password(), adminUser.getPasswordHash())) {
            throw new AdminAuthenticationFailedException("Invalid admin credentials");
        }

        Instant issuedAt = Instant.now(clock);
        adminUser.recordLogin(issuedAt);
        adminUserRepository.save(adminUser);
        return issueAuthentication(adminUser, issuedAt, command.userAgent(), command.ipAddress());
    }

    /**
     * Replaces a valid refresh token with a new token pair.
     *
     * <p>The current token is marked as rotated and revoked in the same transaction. Reuse,
     * revocation, and expiry are treated as business failures.
     */
    @Override
    @Transactional
    public AdminAuthenticationResult refresh(AdminRefreshCommand command) {
        Instant issuedAt = Instant.now(clock);
        String refreshTokenHash = adminRefreshTokenHasher.hash(command.refreshToken());
        AdminRefreshToken currentToken = adminRefreshTokenRepository.findByRefreshTokenHash(refreshTokenHash)
                .orElseThrow(() -> new AdminAuthenticationFailedException("Invalid refresh token"));

        if (currentToken.getReplacedByTokenHash() != null) {
            throw new AdminRefreshTokenReuseException("Refresh token has already been rotated");
        }
        if (currentToken.isRevoked()) {
            throw new AdminAuthenticationFailedException("Refresh token is revoked");
        }
        if (currentToken.isExpiredAt(issuedAt)) {
            throw new AdminAuthenticationFailedException("Refresh token is expired");
        }

        AdminUser adminUser = currentToken.getAdminUser();
        if (!adminUser.isActive()) {
            throw new AdminUserDisabledException(requireAdminUserId(adminUser));
        }

        GeneratedRefreshToken replacementToken = generateRefreshToken();
        currentToken.markRotatedTo(replacementToken.hash());
        currentToken.revoke(issuedAt);
        adminRefreshTokenRepository.save(currentToken);

        return issueAuthentication(adminUser, issuedAt, command.userAgent(), command.ipAddress(), replacementToken);
    }

    /**
     * Revokes the active session represented by the provided refresh token.
     *
     * <p>The operation is intentionally tolerant of already revoked or missing tokens so logout can
     * be retried safely.
     */
    @Override
    @Transactional
    public void logout(AdminLogoutCommand command) {
        adminRefreshTokenRepository.findByRefreshTokenHash(adminRefreshTokenHasher.hash(command.refreshToken()))
                .ifPresent(refreshToken -> revokeIfActive(refreshToken, Instant.now(clock)));
    }

    private void revokeIfActive(AdminRefreshToken refreshToken, Instant revokedAt) {
        if (refreshToken.isRevoked()) {
            return;
        }
        refreshToken.revoke(revokedAt);
        adminRefreshTokenRepository.save(refreshToken);
    }

    private AdminAuthenticationResult issueAuthentication(
            AdminUser adminUser,
            Instant issuedAt,
            String userAgent,
            String ipAddress) {
        return issueAuthentication(adminUser, issuedAt, userAgent, ipAddress, generateRefreshToken());
    }

    private AdminAuthenticationResult issueAuthentication(
            AdminUser adminUser,
            Instant issuedAt,
            String userAgent,
            String ipAddress,
            GeneratedRefreshToken refreshToken) {
        Long adminUserId = requireAdminUserId(adminUser);
        Set<String> roleCodes = adminUser.getRoleAssignments().stream()
                .map(assignment -> assignment.getAdminRole().getCode())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        IssuedAdminAccessToken accessToken = adminJwtService.issueAccessToken(
                new AdminAccessTokenSubject(adminUserId, adminUser.getUsername(), roleCodes),
                issuedAt);

        Instant refreshTokenExpiresAt = issuedAt.plus(adminSecurityProperties.refreshTokenTtl());
        adminRefreshTokenRepository.save(AdminRefreshToken.issue(
                adminUser,
                refreshToken.hash(),
                issuedAt,
                refreshTokenExpiresAt,
                userAgent,
                ipAddress));

        return new AdminAuthenticationResult(
                adminUserId,
                adminUser.getUsername(),
                roleCodes,
                accessToken.tokenValue(),
                accessToken.claims().expiresAt(),
                refreshToken.rawToken(),
                refreshTokenExpiresAt);
    }

    private GeneratedRefreshToken generateRefreshToken() {
        for (int attempt = 0; attempt < REFRESH_TOKEN_GENERATION_ATTEMPTS; attempt++) {
            String rawToken = adminRefreshTokenGenerator.generateToken();
            String tokenHash = adminRefreshTokenHasher.hash(rawToken);
            if (!adminRefreshTokenRepository.existsByRefreshTokenHash(tokenHash)) {
                return new GeneratedRefreshToken(rawToken, tokenHash);
            }
        }
        throw new IllegalStateException("Could not generate a unique admin refresh token");
    }

    private static Long requireAdminUserId(AdminUser adminUser) {
        Long adminUserId = adminUser.getId();
        if (adminUserId == null || adminUserId <= 0) {
            throw new IllegalStateException("Admin user must be persisted before issuing tokens");
        }
        return adminUserId;
    }

    private record GeneratedRefreshToken(String rawToken, String hash) {
    }
}
