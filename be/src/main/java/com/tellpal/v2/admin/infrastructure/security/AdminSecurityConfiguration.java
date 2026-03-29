package com.tellpal.v2.admin.infrastructure.security;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.tellpal.v2.shared.web.admin.AdminAuthenticationFacade;
import com.tellpal.v2.shared.web.admin.AdminRequestLoggingFilter;
import com.tellpal.v2.shared.web.admin.AdminWebRequestSupport;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AdminSecurityProperties.class)
public class AdminSecurityConfiguration {

    private static final Logger log = LoggerFactory.getLogger(AdminSecurityConfiguration.class);

    @Bean
    PasswordEncoder adminPasswordEncoder(AdminSecurityProperties properties) {
        return new BCryptPasswordEncoder(properties.bcryptStrength());
    }

    @Bean
    JwtEncoder adminJwtEncoder(AdminSecurityProperties properties) {
        return NimbusJwtEncoder.withSecretKey(properties.jwtSecretKey()).build();
    }

    @Bean
    JwtDecoder adminJwtDecoder(AdminSecurityProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(properties.jwtSecretKey())
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.jwtIssuer()));
        return decoder;
    }

    @Bean
    @Order(1)
    SecurityFilterChain adminSecurityFilterChain(
            HttpSecurity http,
            AdminJwtAuthenticationConverter authenticationConverter,
            AdminRequestLoggingFilter adminRequestLoggingFilter,
            AuthenticationEntryPoint adminAuthenticationEntryPoint,
            AccessDeniedHandler adminAccessDeniedHandler) throws Exception {
        http.securityMatcher("/api/admin/**")
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .rememberMe(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/admin/auth/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(adminAuthenticationEntryPoint)
                        .accessDeniedHandler(adminAccessDeniedHandler))
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(authenticationConverter)))
                .securityContext(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .anonymous(Customizer.withDefaults())
                .addFilterAfter(adminRequestLoggingFilter, AuthorizationFilter.class);

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(AdminSecurityProperties properties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(properties.cors().allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of(AdminWebRequestSupport.REQUEST_ID_HEADER));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/admin/**", configuration);
        return source;
    }

    @Bean
    AdminRequestLoggingFilter adminRequestLoggingFilter(AdminAuthenticationFacade authenticationFacade) {
        return new AdminRequestLoggingFilter(authenticationFacade);
    }

    @Bean
    AuthenticationEntryPoint adminAuthenticationEntryPoint() {
        return (request, response, exception) -> {
            log.warn(
                    "admin_auth_failed status={} method={} path={} clientIp={} requestId={} reason={}",
                    HttpStatus.UNAUTHORIZED.value(),
                    request.getMethod(),
                    request.getRequestURI(),
                    AdminWebRequestSupport.resolveClientIp(request),
                    AdminWebRequestSupport.resolveRequestId(request),
                    exception.getMessage());
            response.sendError(HttpStatus.UNAUTHORIZED.value(), HttpStatus.UNAUTHORIZED.getReasonPhrase());
        };
    }

    @Bean
    AccessDeniedHandler adminAccessDeniedHandler() {
        return (request, response, exception) -> {
            log.warn(
                    "admin_auth_failed status={} method={} path={} clientIp={} requestId={} reason={}",
                    HttpStatus.FORBIDDEN.value(),
                    request.getMethod(),
                    request.getRequestURI(),
                    AdminWebRequestSupport.resolveClientIp(request),
                    AdminWebRequestSupport.resolveRequestId(request),
                    exception.getMessage());
            response.sendError(HttpStatus.FORBIDDEN.value(), HttpStatus.FORBIDDEN.getReasonPhrase());
        };
    }
}
