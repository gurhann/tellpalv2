package com.tellpal.v2.admin.infrastructure.security;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
public class AdminJwtService {

    static final String CLAIM_USERNAME = "username";
    static final String CLAIM_ROLES = "roles";

    private final Clock clock;
    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final AdminSecurityProperties properties;

    public AdminJwtService(
            Clock clock,
            JwtEncoder jwtEncoder,
            JwtDecoder jwtDecoder,
            AdminSecurityProperties properties) {
        this.clock = clock;
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
        this.properties = properties;
    }

    public IssuedAdminAccessToken issueAccessToken(AdminAccessTokenSubject subject) {
        return issueAccessToken(subject, Instant.now(clock));
    }

    public IssuedAdminAccessToken issueAccessToken(AdminAccessTokenSubject subject, Instant issuedAt) {
        if (subject == null) {
            throw new IllegalArgumentException("Admin access token subject must not be null");
        }
        if (issuedAt == null) {
            throw new IllegalArgumentException("Admin access token issue time must not be null");
        }

        Instant expiresAt = issuedAt.plus(properties.accessTokenTtl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.jwtIssuer())
                .subject(subject.adminUserId().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim(CLAIM_USERNAME, subject.username())
                .claim(CLAIM_ROLES, subject.roleCodes().stream().sorted().toList())
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        String tokenValue = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedAdminAccessToken(tokenValue, toClaims(subject.adminUserId(), subject.username(), claims));
    }

    public AdminAccessTokenClaims decodeAccessToken(String tokenValue) {
        Jwt jwt = jwtDecoder.decode(requireText(tokenValue, "Admin access token must not be blank"));
        return toClaims(parseAdminUserId(jwt.getSubject()), jwt.getClaimAsString(CLAIM_USERNAME), jwt);
    }

    private AdminAccessTokenClaims toClaims(Long adminUserId, String username, JwtClaimsSet claims) {
        return new AdminAccessTokenClaims(
                adminUserId,
                username,
                Set.copyOf(claims.getClaimAsStringList(CLAIM_ROLES)),
                claims.getIssuedAt(),
                claims.getExpiresAt());
    }

    private AdminAccessTokenClaims toClaims(Long adminUserId, String username, Jwt jwt) {
        List<String> roleCodes = jwt.getClaimAsStringList(CLAIM_ROLES);
        return new AdminAccessTokenClaims(
                adminUserId,
                requireText(username, "Admin username claim must not be blank"),
                roleCodes == null ? Set.of() : Set.copyOf(roleCodes),
                jwt.getIssuedAt(),
                jwt.getExpiresAt());
    }

    private static Long parseAdminUserId(String subject) {
        String tokenSubject = requireText(subject, "Admin token subject must not be blank");
        try {
            return Long.parseLong(tokenSubject);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Admin token subject must be a numeric user ID", exception);
        }
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
