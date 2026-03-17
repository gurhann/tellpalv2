package com.tellpal.v2.admin.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class AdminPasswordHasherTest {

    private static final AdminSecurityProperties PROPERTIES = new AdminSecurityProperties(
            "tellpal-v2-admin",
            "change-me-change-me-change-me-32-bytes",
            Duration.ofHours(1),
            Duration.ofDays(30),
            10);

    private final AdminPasswordHasher passwordHasher = new AdminPasswordHasher(
            new AdminSecurityConfiguration().adminPasswordEncoder(PROPERTIES));

    @Test
    void hashesAndVerifiesPasswords() {
        String passwordHash = passwordHasher.hash("S3curePassword!");

        assertThat(passwordHash).isNotBlank();
        assertThat(passwordHash).isNotEqualTo("S3curePassword!");
        assertThat(passwordHasher.matches("S3curePassword!", passwordHash)).isTrue();
        assertThat(passwordHasher.matches("wrong-password", passwordHash)).isFalse();
    }

    @Test
    void rejectsBlankPasswords() {
        assertThatThrownBy(() -> passwordHasher.hash(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be blank");
    }
}
