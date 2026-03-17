package com.tellpal.v2.purchase.infrastructure.revenuecat;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
class RevenueCatSecurityConfigurationValidator {

    private final Environment environment;
    private final String authorizationHeader;

    RevenueCatSecurityConfigurationValidator(
            Environment environment,
            @Value("${tellpal.purchase.revenuecat.authorization-header:}") String authorizationHeader) {
        this.environment = environment;
        this.authorizationHeader = authorizationHeader == null ? "" : authorizationHeader.trim();
    }

    @PostConstruct
    void validate() {
        if (environment.matchesProfiles("local", "test")) {
            return;
        }
        if (authorizationHeader.isBlank()) {
            throw new IllegalStateException("RevenueCat authorization header must be configured outside local/test");
        }
    }
}
