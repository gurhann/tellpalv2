package com.tellpal.v2.shared.infrastructure.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.micrometer.metrics.autoconfigure.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class MetricsConfiguration {

    @Bean
    MeterRegistryCustomizer<MeterRegistry> tellPalMeterRegistryCustomizer(
            @Value("${spring.application.name}") String applicationName) {
        return registry -> registry.config()
                .meterFilter(MeterFilter.denyNameStartsWith("tomcat.sessions"))
                .commonTags("application", applicationName);
    }
}
