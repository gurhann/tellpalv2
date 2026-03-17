package com.tellpal.v2.user.infrastructure.firebase;

import jakarta.annotation.PostConstruct;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
class FirebaseSecurityConfigurationValidator {

    private final Environment environment;
    private final FirebaseAuthenticationProperties firebaseAuthenticationProperties;

    FirebaseSecurityConfigurationValidator(
            Environment environment,
            FirebaseAuthenticationProperties firebaseAuthenticationProperties) {
        this.environment = environment;
        this.firebaseAuthenticationProperties = firebaseAuthenticationProperties;
    }

    @PostConstruct
    void validate() {
        if (environment.matchesProfiles("local", "test")) {
            return;
        }
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

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
