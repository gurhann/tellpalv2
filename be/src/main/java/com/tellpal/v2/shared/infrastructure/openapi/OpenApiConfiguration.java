package com.tellpal.v2.shared.infrastructure.openapi;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * Registers grouped OpenAPI documentation when API docs are explicitly enabled.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ApiDocumentationProperties.class)
@ConditionalOnProperty(prefix = "tellpal.api-docs", name = "enabled", havingValue = "true")
public class OpenApiConfiguration {

    @Bean
    OpenAPI tellpalOpenApi(ApiDocumentationProperties properties) {
        return new OpenAPI()
                .info(new Info()
                        .title(properties.title())
                        .description(properties.description())
                        .version(properties.version()))
                .components(new Components()
                        .addSecuritySchemes("adminBearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Admin access token"))
                        .addSecuritySchemes("mobileBearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("Firebase ID token")
                                .description("Mobile Firebase bearer token"))
                        .addSecuritySchemes("revenueCatHeaderAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("Authorization")
                                .description("RevenueCat authorization header"))
                        .addSchemas("ProblemDetail", new ObjectSchema()
                                .addProperty("type", new StringSchema())
                                .addProperty("title", new StringSchema())
                                .addProperty("status", new IntegerSchema())
                                .addProperty("detail", new StringSchema())
                                .addProperty("instance", new StringSchema())
                                .addProperty("errorCode", new StringSchema())
                                .addProperty("path", new StringSchema())
                                .addProperty("requestId", new StringSchema())
                                .addProperty("adminUserId", new StringSchema())
                                .addProperty("fieldErrors", new MapSchema().additionalProperties(new StringSchema()))));
    }

    @Bean
    GroupedOpenApi adminOpenApi() {
        return GroupedOpenApi.builder()
                .group("admin")
                .pathsToMatch("/api/admin/**")
                .build();
    }

    @Bean
    GroupedOpenApi mobileOpenApi() {
        return GroupedOpenApi.builder()
                .group("mobile")
                .pathsToMatch(
                        "/api/categories",
                        "/api/categories/**",
                        "/api/contents",
                        "/api/contents/**",
                        "/api/profiles",
                        "/api/profiles/**",
                        "/api/events",
                        "/api/events/**")
                .build();
    }

    @Bean
    GroupedOpenApi webhookOpenApi() {
        return GroupedOpenApi.builder()
                .group("webhook")
                .pathsToMatch("/api/webhooks/**")
                .build();
    }
}
