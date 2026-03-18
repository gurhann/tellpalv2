package com.tellpal.v2.shared.infrastructure.json;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * Registers the shared Jackson mapper when the application has not provided one explicitly.
 */
@Configuration(proxyBeanMethods = false)
public class JacksonConfiguration {

    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    ObjectMapper objectMapper() {
        return JsonMapper.builder()
                .findAndAddModules()
                .build();
    }
}
