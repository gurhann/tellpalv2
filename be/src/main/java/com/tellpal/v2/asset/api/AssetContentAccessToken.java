package com.tellpal.v2.asset.api;

import java.time.Instant;

/**
 * Short-lived token that authorizes backend streaming for one media asset.
 */
public record AssetContentAccessToken(
        String token,
        Instant expiresAt) {

    public AssetContentAccessToken {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Asset content token must not be blank");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("Asset content token expiry must not be null");
        }
        token = token.trim();
    }
}
