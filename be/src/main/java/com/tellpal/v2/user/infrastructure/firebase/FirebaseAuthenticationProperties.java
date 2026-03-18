package com.tellpal.v2.user.infrastructure.firebase;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalized settings that control how mobile Firebase tokens are verified.
 */
@ConfigurationProperties("tellpal.security.firebase")
public record FirebaseAuthenticationProperties(
        boolean stubTokensEnabled,
        String projectId,
        String credentialsPath,
        boolean checkRevoked) {
}
