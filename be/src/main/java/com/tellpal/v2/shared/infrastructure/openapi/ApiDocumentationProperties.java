package com.tellpal.v2.shared.infrastructure.openapi;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalized settings for exposing generated OpenAPI documentation.
 */
@ConfigurationProperties("tellpal.api-docs")
public record ApiDocumentationProperties(
        boolean enabled,
        String title,
        String description,
        String version) {
}
