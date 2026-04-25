package com.tellpal.v2.admin.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtException;

class AdminJwtServiceTest {

    private static final Instant FIXED_NOW = Instant.now().plus(Duration.ofMinutes(5)).truncatedTo(ChronoUnit.SECONDS);
    private static final AdminSecurityProperties PROPERTIES = new AdminSecurityProperties(
            "tellpal-v2-admin",
            "change-me-change-me-change-me-32-bytes",
            Duration.ofHours(1),
            Duration.ofDays(30),
            10,
            new AdminSecurityProperties.BruteForceProperties(
                    true,
                    Duration.ofMinutes(15),
                    Duration.ofMinutes(15),
                    5,
                    20,
                    30,
                    10_000),
            new AdminSecurityProperties.CorsProperties(java.util.List.of()));

    private final AdminSecurityConfiguration configuration = new AdminSecurityConfiguration();
    private final Clock clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
    private final AdminJwtService jwtService = new AdminJwtService(
            clock,
            configuration.adminJwtEncoder(PROPERTIES),
            configuration.adminJwtDecoder(PROPERTIES),
            PROPERTIES);

    @Test
    void issuesAndDecodesAccessTokens() {
        AdminAccessTokenSubject subject = new AdminAccessTokenSubject(42L, "admin-root", Set.of("ADMIN", "VIEWER"));

        IssuedAdminAccessToken issuedToken = jwtService.issueAccessToken(subject);
        AdminAccessTokenClaims decodedClaims = jwtService.decodeAccessToken(issuedToken.tokenValue());

        assertThat(issuedToken.tokenValue()).isNotBlank();
        assertThat(issuedToken.claims().adminUserId()).isEqualTo(42L);
        assertThat(issuedToken.claims().username()).isEqualTo("admin-root");
        assertThat(issuedToken.claims().roleCodes()).containsExactlyInAnyOrder("ADMIN", "VIEWER");
        assertThat(issuedToken.claims().issuedAt()).isEqualTo(FIXED_NOW);
        assertThat(issuedToken.claims().expiresAt()).isEqualTo(FIXED_NOW.plus(PROPERTIES.accessTokenTtl()));

        assertThat(decodedClaims).isEqualTo(issuedToken.claims());
    }

    @Test
    void rejectsMalformedTokens() {
        assertThatThrownBy(() -> jwtService.decodeAccessToken("not-a-jwt"))
                .isInstanceOf(JwtException.class);
    }
}
