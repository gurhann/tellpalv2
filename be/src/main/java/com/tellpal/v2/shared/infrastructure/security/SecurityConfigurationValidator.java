package com.tellpal.v2.shared.infrastructure.security;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.tellpal.v2.admin.infrastructure.security.AdminSecurityProperties;
import com.tellpal.v2.user.infrastructure.firebase.FirebaseAuthenticationProperties;

@Component
public class SecurityConfigurationValidator {

    private static final String INSECURE_LOCAL_ADMIN_SECRET = "change-me-change-me-change-me-32-bytes";

    private final Environment environment;
    private final AdminSecurityProperties adminSecurityProperties;
    private final FirebaseAuthenticationProperties firebaseAuthenticationProperties;
    private final String revenueCatAuthorizationHeader;

    public SecurityConfigurationValidator(
            Environment environment,
            AdminSecurityProperties adminSecurityProperties,
            FirebaseAuthenticationProperties firebaseAuthenticationProperties,
            @Value("${tellpal.purchase.revenuecat.authorization-header:}") String revenueCatAuthorizationHeader) {
        this.environment = environment;
        this.adminSecurityProperties = adminSecurityProperties;
        this.firebaseAuthenticationProperties = firebaseAuthenticationProperties;
        this.revenueCatAuthorizationHeader = revenueCatAuthorizationHeader == null
                ? ""
                : revenueCatAuthorizationHeader.trim();
    }

    @PostConstruct
    void validate() {
        if (environment.matchesProfiles("local", "test")) {
            return;
        }
        validateAdminSecret();
        validateFirebaseConfiguration();
        validateRevenueCatConfiguration();
    }

    private void validateAdminSecret() {
        String secret = adminSecurityProperties.jwtSecret();
        if (secret.isBlank() || INSECURE_LOCAL_ADMIN_SECRET.equals(secret)) {
            throw new IllegalStateException("Admin JWT secret must be configured with a non-default value");
        }
    }

    private void validateFirebaseConfiguration() {
        if (firebaseAuthenticationProperties.stubTokensEnabled()) {
            throw new IllegalStateException("Firebase stub token verification must be disabled outside local/test");
        }
        if (isBlank(firebaseAuthenticationProperties.projectId())) {
            throw new IllegalStateException("Firebase project ID must be configured outside local/test");
        }
        if (isBlank(firebaseAuthenticationProperties.credentialsPath())) {
            throw new IllegalStateException("Firebase credentials path must be configured outside local/test");
        }
    }

    private void validateRevenueCatConfiguration() {
        if (revenueCatAuthorizationHeader.isBlank()) {
            throw new IllegalStateException("RevenueCat authorization header must be configured outside local/test");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
