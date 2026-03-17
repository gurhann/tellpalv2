package com.tellpal.v2.admin.infrastructure.security;

import jakarta.annotation.PostConstruct;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
class AdminSecurityConfigurationValidator {

    private static final String INSECURE_LOCAL_ADMIN_SECRET = "change-me-change-me-change-me-32-bytes";

    private final Environment environment;
    private final AdminSecurityProperties adminSecurityProperties;

    AdminSecurityConfigurationValidator(Environment environment, AdminSecurityProperties adminSecurityProperties) {
        this.environment = environment;
        this.adminSecurityProperties = adminSecurityProperties;
    }

    @PostConstruct
    void validate() {
        if (environment.matchesProfiles("local", "test")) {
            return;
        }
        if (INSECURE_LOCAL_ADMIN_SECRET.equals(adminSecurityProperties.jwtSecret())) {
            throw new IllegalStateException("Admin JWT secret must be configured with a non-default value");
        }
    }
}
