package com.tellpal.v2.admin.infrastructure.security;

public record IssuedAdminAccessToken(String tokenValue, AdminAccessTokenClaims claims) {

    public IssuedAdminAccessToken {
        if (tokenValue == null || tokenValue.isBlank()) {
            throw new IllegalArgumentException("Admin access token value must not be blank");
        }
        if (claims == null) {
            throw new IllegalArgumentException("Admin access token claims must not be null");
        }
    }
}
