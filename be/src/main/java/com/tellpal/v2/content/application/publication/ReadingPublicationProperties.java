package com.tellpal.v2.content.application.publication;

import java.util.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.core.env.Environment;
import com.tellpal.v2.asset.api.ReadingImagePreparationApi.*;

/** Every role profile is supplied explicitly. Default and production activation are closed. */
@Component
@ConfigurationProperties(prefix = "tellpal.content.reading-publication")
public class ReadingPublicationProperties {
    private boolean enabled;
    private Map<Role, TestProfile> profiles = new EnumMap<>(Role.class);
    private final Environment environment;
    public ReadingPublicationProperties(Environment environment) {
        this.environment = environment;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setProfiles(Map<Role, TestProfile> profiles) {
        this.profiles = new EnumMap<>(profiles);
    }

    public Map<Role, TestProfile> profiles() {
        return Map.copyOf(profiles);
    }

    public boolean enabled() {
        var active = Set.of(environment.getActiveProfiles());
        return enabled && (active.contains("local") || active.contains("test")) && active.stream().noneMatch(p -> p.equalsIgnoreCase("prod") || p.equalsIgnoreCase("production")) && profiles.keySet().equals(Set.of(Role.values()));
    }
}
