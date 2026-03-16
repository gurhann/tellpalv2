package com.tellpal.v2.admin.application;

import com.tellpal.v2.admin.domain.AdminRefreshToken;
import com.tellpal.v2.admin.domain.AdminRefreshTokenRepository;
import com.tellpal.v2.admin.domain.AdminRole;
import com.tellpal.v2.admin.domain.AdminRoleRepository;
import com.tellpal.v2.admin.domain.AdminUser;
import com.tellpal.v2.admin.domain.AdminUserRepository;
import com.tellpal.v2.admin.domain.AdminUserRole;
import com.tellpal.v2.admin.domain.AdminUserRoleId;
import com.tellpal.v2.admin.domain.AdminUserRoleRepository;
import com.tellpal.v2.admin.infrastructure.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@Transactional
public class AdminAuthApplicationService {

    private final AdminUserRepository adminUserRepository;
    private final AdminRoleRepository adminRoleRepository;
    private final AdminRefreshTokenRepository adminRefreshTokenRepository;
    private final AdminUserRoleRepository adminUserRoleRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    public AdminAuthApplicationService(
            AdminUserRepository adminUserRepository,
            AdminRoleRepository adminRoleRepository,
            AdminRefreshTokenRepository adminRefreshTokenRepository,
            AdminUserRoleRepository adminUserRoleRepository,
            JwtTokenProvider jwtTokenProvider,
            PasswordEncoder passwordEncoder) {
        this.adminUserRepository = adminUserRepository;
        this.adminRoleRepository = adminRoleRepository;
        this.adminRefreshTokenRepository = adminRefreshTokenRepository;
        this.adminUserRoleRepository = adminUserRoleRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResult login(String username, String password) {
        AdminUser user = adminUserRepository.findByUsername(username)
                .orElseThrow(() -> new AdminAuthException("User not found: " + username));

        if (!user.isEnabled()) {
            throw new AdminAuthException("User account is disabled: " + username);
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new AdminAuthException("Invalid credentials");
        }

        String accessToken = jwtTokenProvider.generateAccessToken(username);
        String refreshToken = jwtTokenProvider.generateRefreshToken();
        String refreshTokenHash = jwtTokenProvider.hashToken(refreshToken);

        OffsetDateTime now = OffsetDateTime.now();
        AdminRefreshToken tokenEntity = new AdminRefreshToken(
                user.getId(),
                refreshTokenHash,
                now,
                now.plusDays(30),
                null,
                null
        );
        adminRefreshTokenRepository.save(tokenEntity);

        user.setLastLoginAt(now);
        adminUserRepository.save(user);

        return new LoginResult(accessToken, refreshToken);
    }

    public RefreshResult refreshAccessToken(String refreshToken) {
        String hash = jwtTokenProvider.hashToken(refreshToken);

        AdminRefreshToken oldToken = adminRefreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new AdminAuthException("Refresh token not found"));

        if (!oldToken.isValid()) {
            throw new AdminAuthException("Refresh token is expired or revoked");
        }

        AdminUser user = adminUserRepository.findById(oldToken.getAdminUserId())
                .orElseThrow(() -> new AdminAuthException("User not found for token"));

        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getUsername());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken();
        String newRefreshTokenHash = jwtTokenProvider.hashToken(newRefreshToken);

        oldToken.setRevokedAt(OffsetDateTime.now());
        oldToken.setReplacedByTokenHash(newRefreshTokenHash);
        adminRefreshTokenRepository.save(oldToken);

        OffsetDateTime now = OffsetDateTime.now();
        AdminRefreshToken newTokenEntity = new AdminRefreshToken(
                user.getId(),
                newRefreshTokenHash,
                now,
                now.plusDays(30),
                null,
                null
        );
        adminRefreshTokenRepository.save(newTokenEntity);

        return new RefreshResult(newAccessToken, newRefreshToken);
    }

    public void logout(String refreshToken) {
        String hash = jwtTokenProvider.hashToken(refreshToken);
        adminRefreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
            token.setRevokedAt(OffsetDateTime.now());
            adminRefreshTokenRepository.save(token);
        });
    }

    public AdminUser createAdminUser(String username, String rawPassword) {
        String encodedPassword = passwordEncoder.encode(rawPassword);
        AdminUser user = new AdminUser(username, encodedPassword);
        return adminUserRepository.save(user);
    }

    public void assignRole(Long adminUserId, String roleCode) {
        adminUserRepository.findById(adminUserId)
                .orElseThrow(() -> new AdminAuthException("Admin user not found: " + adminUserId));

        adminRoleRepository.findById(roleCode)
                .orElseThrow(() -> new AdminAuthException("Role not found: " + roleCode));

        AdminUserRole userRole = new AdminUserRole(adminUserId, roleCode);
        adminUserRoleRepository.save(userRole);
    }

    public void removeRole(Long adminUserId, String roleCode) {
        AdminUserRoleId id = new AdminUserRoleId(adminUserId, roleCode);
        adminUserRoleRepository.deleteById(id);
    }
}
