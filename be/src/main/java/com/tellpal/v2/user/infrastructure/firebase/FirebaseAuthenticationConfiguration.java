package com.tellpal.v2.user.infrastructure.firebase;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.tellpal.v2.user.application.FirebaseTokenVerifier;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(FirebaseAuthenticationProperties.class)
public class FirebaseAuthenticationConfiguration {

    @Bean
    FirebaseTokenVerifier firebaseTokenVerifier(FirebaseAuthenticationProperties properties) {
        if (properties.stubTokensEnabled()) {
            return new StubFirebaseTokenVerifier();
        }
        return new FirebaseAdminTokenVerifier(properties);
    }
}
