package com.tellpal.v2.shared;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.Size;
import net.jqwik.spring.JqwikSpringSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based integration test for language seed data integrity.
 *
 * Validates: Requirements 21.1, 21.2, 21.3
 *
 * Özellik 32: Dil Seed Verisi Bütünlüğü
 * - tr, en, es, pt, de dil kayıtları mevcut olmalı
 * - Tüm kayıtlar is_active = true olmalı
 * - display_name null veya boş olmamalı
 */
@SpringBootTest
@Testcontainers
@JqwikSpringSupport
public class LanguageSeedIntegrityTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @Autowired
    JdbcTemplate jdbcTemplate;

    /**
     * Provides the 5 expected language codes as an Arbitrary.
     * **Validates: Requirements 21.1**
     */
    @Provide
    Arbitrary<String> expectedLanguageCodes() {
        return Arbitraries.of("tr", "en", "es", "pt", "de");
    }

    /**
     * Property: For every expected language code, a record exists in v2_languages
     * with is_active = true and a non-null, non-empty display_name.
     *
     * **Validates: Requirements 21.1, 21.2, 21.3**
     */
    @Property(tries = 5)
    void eachExpectedLanguageExistsWithActiveStatusAndDisplayName(
            @ForAll("expectedLanguageCodes") String code) {

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM v2_languages WHERE code = ?",
                Integer.class,
                code);
        assertThat(count)
                .as("Language record should exist for code: %s", code)
                .isEqualTo(1);

        Boolean isActive = jdbcTemplate.queryForObject(
                "SELECT is_active FROM v2_languages WHERE code = ?",
                Boolean.class,
                code);
        assertThat(isActive)
                .as("Language %s should have is_active = true", code)
                .isTrue();

        String displayName = jdbcTemplate.queryForObject(
                "SELECT display_name FROM v2_languages WHERE code = ?",
                String.class,
                code);
        assertThat(displayName)
                .as("Language %s should have a non-null, non-empty display_name", code)
                .isNotNull()
                .isNotBlank();
    }

    /**
     * Unit test: Exactly 5 language records exist in total.
     *
     * **Validates: Requirements 21.1**
     */
    @Test
    void exactlyFiveLanguagesExist() {
        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM v2_languages",
                Integer.class);
        assertThat(total)
                .as("Exactly 5 language records should be seeded")
                .isEqualTo(5);
    }
}
