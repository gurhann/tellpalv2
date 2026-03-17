package com.tellpal.v2.admin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.tellpal.v2.admin.api.AdminAuthenticationResult;
import com.tellpal.v2.admin.api.AdminLoginCommand;
import com.tellpal.v2.admin.api.AdminLogoutCommand;
import com.tellpal.v2.admin.api.AdminRefreshCommand;
import com.tellpal.v2.admin.domain.AdminRefreshToken;
import com.tellpal.v2.admin.domain.AdminRefreshTokenRepository;
import com.tellpal.v2.admin.domain.AdminRole;
import com.tellpal.v2.admin.domain.AdminUser;
import com.tellpal.v2.admin.domain.AdminUserRepository;
import com.tellpal.v2.admin.infrastructure.security.AdminAccessTokenClaims;
import com.tellpal.v2.admin.infrastructure.security.AdminAccessTokenSubject;
import com.tellpal.v2.admin.infrastructure.security.AdminJwtService;
import com.tellpal.v2.admin.infrastructure.security.AdminPasswordHasher;
import com.tellpal.v2.admin.infrastructure.security.AdminRefreshTokenGenerator;
import com.tellpal.v2.admin.infrastructure.security.AdminRefreshTokenHasher;
import com.tellpal.v2.admin.infrastructure.security.AdminSecurityProperties;
import com.tellpal.v2.admin.infrastructure.security.IssuedAdminAccessToken;

@ExtendWith(MockitoExtension.class)
class AdminAuthenticationServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-03-17T10:00:00Z");

    @Mock
    private AdminUserRepository adminUserRepository;

    @Mock
    private AdminRefreshTokenRepository adminRefreshTokenRepository;

    @Mock
    private AdminPasswordHasher adminPasswordHasher;

    @Mock
    private AdminRefreshTokenHasher adminRefreshTokenHasher;

    @Mock
    private AdminRefreshTokenGenerator adminRefreshTokenGenerator;

    @Mock
    private AdminJwtService adminJwtService;

    private AdminAuthenticationService adminAuthenticationService;

    @BeforeEach
    void setUp() {
        adminAuthenticationService = new AdminAuthenticationService(
                Clock.fixed(FIXED_NOW, ZoneOffset.UTC),
                adminUserRepository,
                adminRefreshTokenRepository,
                adminPasswordHasher,
                adminRefreshTokenHasher,
                adminRefreshTokenGenerator,
                adminJwtService,
                new AdminSecurityProperties(
                        "tellpal-v2-admin",
                        "change-me-change-me-change-me-32-bytes",
                        Duration.ofHours(1),
                        Duration.ofDays(30),
                        10));
    }

    @Test
    void loginIssuesAccessAndRefreshTokens() {
        AdminUser adminUser = persistedAdminUser(42L, "admin-root", "stored-password-hash", Set.of("ADMIN"));
        when(adminUserRepository.findByUsername("admin-root")).thenReturn(Optional.of(adminUser));
        when(adminPasswordHasher.matches("s3cret", "stored-password-hash")).thenReturn(true);
        when(adminRefreshTokenGenerator.generateToken()).thenReturn("raw-refresh-token");
        when(adminRefreshTokenHasher.hash("raw-refresh-token")).thenReturn("hashed-refresh-token");
        when(adminRefreshTokenRepository.existsByRefreshTokenHash("hashed-refresh-token")).thenReturn(false);
        when(adminJwtService.issueAccessToken(any(AdminAccessTokenSubject.class), any(Instant.class))).thenReturn(
                new IssuedAdminAccessToken(
                        "signed-access-token",
                        new AdminAccessTokenClaims(
                                42L,
                                "admin-root",
                                Set.of("ADMIN"),
                                FIXED_NOW,
                                FIXED_NOW.plus(Duration.ofHours(1)))));

        AdminAuthenticationResult result = adminAuthenticationService.login(
                new AdminLoginCommand("admin-root", "s3cret", "Mozilla", "127.0.0.1"));

        assertThat(result.adminUserId()).isEqualTo(42L);
        assertThat(result.roleCodes()).containsExactly("ADMIN");
        assertThat(result.accessToken()).isEqualTo("signed-access-token");
        assertThat(result.refreshToken()).isEqualTo("raw-refresh-token");
        assertThat(result.refreshTokenExpiresAt()).isEqualTo(FIXED_NOW.plus(Duration.ofDays(30)));
        assertThat(adminUser.getLastLoginAt()).isEqualTo(FIXED_NOW);
        verify(adminUserRepository).save(adminUser);
        verify(adminRefreshTokenRepository).save(any(AdminRefreshToken.class));
    }

    @Test
    void refreshRotatesTokensAndRejectsReuse() {
        AdminUser adminUser = persistedAdminUser(42L, "admin-root", "stored-password-hash", Set.of("ADMIN", "VIEWER"));
        AdminRefreshToken currentToken = AdminRefreshToken.issue(
                adminUser,
                "current-token-hash",
                FIXED_NOW.minus(Duration.ofMinutes(5)),
                FIXED_NOW.plus(Duration.ofDays(30)),
                "Mozilla",
                "127.0.0.1");

        when(adminRefreshTokenHasher.hash("current-raw-token")).thenReturn("current-token-hash");
        when(adminRefreshTokenRepository.findByRefreshTokenHash("current-token-hash")).thenReturn(Optional.of(currentToken));
        when(adminRefreshTokenGenerator.generateToken()).thenReturn("replacement-raw-token");
        when(adminRefreshTokenHasher.hash("replacement-raw-token")).thenReturn("replacement-token-hash");
        when(adminRefreshTokenRepository.existsByRefreshTokenHash("replacement-token-hash")).thenReturn(false);
        when(adminJwtService.issueAccessToken(any(AdminAccessTokenSubject.class), any(Instant.class))).thenReturn(
                new IssuedAdminAccessToken(
                        "replacement-access-token",
                        new AdminAccessTokenClaims(
                                42L,
                                "admin-root",
                                Set.of("ADMIN", "VIEWER"),
                                FIXED_NOW,
                                FIXED_NOW.plus(Duration.ofHours(1)))));

        AdminAuthenticationResult refreshed = adminAuthenticationService.refresh(
                new AdminRefreshCommand("current-raw-token", "Mozilla", "127.0.0.1"));

        assertThat(refreshed.refreshToken()).isEqualTo("replacement-raw-token");
        assertThat(currentToken.getReplacedByTokenHash()).isEqualTo("replacement-token-hash");
        assertThat(currentToken.getRevokedAt()).isEqualTo(FIXED_NOW);

        assertThatThrownBy(() -> adminAuthenticationService.refresh(
                new AdminRefreshCommand("current-raw-token", "Mozilla", "127.0.0.1")))
                .isInstanceOf(AdminRefreshTokenReuseException.class);
    }

    @Test
    void logoutRevokesExistingRefreshToken() {
        AdminUser adminUser = persistedAdminUser(42L, "admin-root", "stored-password-hash", Set.of("ADMIN"));
        AdminRefreshToken refreshToken = AdminRefreshToken.issue(
                adminUser,
                "current-token-hash",
                FIXED_NOW.minus(Duration.ofMinutes(5)),
                FIXED_NOW.plus(Duration.ofDays(30)),
                "Mozilla",
                "127.0.0.1");

        when(adminRefreshTokenHasher.hash("current-raw-token")).thenReturn("current-token-hash");
        when(adminRefreshTokenRepository.findByRefreshTokenHash("current-token-hash")).thenReturn(Optional.of(refreshToken));

        adminAuthenticationService.logout(new AdminLogoutCommand("current-raw-token"));

        assertThat(refreshToken.getRevokedAt()).isEqualTo(FIXED_NOW);
        verify(adminRefreshTokenRepository).save(refreshToken);
    }

    @Test
    void loginRejectsDisabledUsers() {
        AdminUser disabledUser = persistedAdminUser(42L, "disabled-admin", "stored-password-hash", Set.of("ADMIN"));
        disabledUser.deactivate();
        when(adminUserRepository.findByUsername("disabled-admin")).thenReturn(Optional.of(disabledUser));

        assertThatThrownBy(() -> adminAuthenticationService.login(
                new AdminLoginCommand("disabled-admin", "s3cret", null, null)))
                .isInstanceOf(AdminUserDisabledException.class);

        verify(adminPasswordHasher, never()).matches(any(), any());
    }

    private static AdminUser persistedAdminUser(
            Long adminUserId,
            String username,
            String passwordHash,
            Set<String> roleCodes) {
        AdminUser adminUser = AdminUser.create(username, passwordHash);
        ReflectionTestUtils.setField(adminUser, "id", adminUserId, Long.class);

        for (String roleCode : roleCodes) {
            AdminRole adminRole = AdminRole.create(roleCode, roleCode + " description");
            adminUser.assignRole(adminRole, FIXED_NOW.minus(Duration.ofHours(1)));
        }

        return adminUser;
    }
}
