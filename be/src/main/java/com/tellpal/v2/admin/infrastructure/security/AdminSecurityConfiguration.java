package com.tellpal.v2.admin.infrastructure.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.SecurityFilterChain;

import com.tellpal.v2.shared.web.admin.AdminAuthenticationFacade;
import com.tellpal.v2.shared.web.admin.AdminRequestLoggingFilter;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AdminSecurityProperties.class)
public class AdminSecurityConfiguration {

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
            AdminRequestLoggingFilter adminRequestLoggingFilter) throws Exception {
        http.securityMatcher("/api/admin/**")
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .rememberMe(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/admin/auth/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(authenticationConverter)))
                .securityContext(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .anonymous(Customizer.withDefaults())
                .addFilterAfter(adminRequestLoggingFilter, AuthorizationFilter.class);

        return http.build();
    }

    @Bean
    AdminRequestLoggingFilter adminRequestLoggingFilter(AdminAuthenticationFacade authenticationFacade) {
        return new AdminRequestLoggingFilter(authenticationFacade);
    }
}
