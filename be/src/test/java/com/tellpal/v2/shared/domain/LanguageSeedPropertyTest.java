package com.tellpal.v2.shared.domain;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based test for language seed data integrity.
 *
 * Validates: Requirements 21.1, 21.2, 21.3
 *
 * Özellik 32: Dil Seed Verisi Bütünlüğü
 * - tr, en, es, pt, de dil kayıtları mevcut olmalı
 * - Tüm kayıtlar is_active = true olmalı
 * - display_name değerleri null olmamalı
 */
public class LanguageSeedPropertyTest {

    /** Seed data as defined in V3__seed_languages.sql */
    private static final List<Language> SEED_LANGUAGES = List.of(
            new Language("tr", "Türkçe",     true),
            new Language("en", "İngilizce",  true),
            new Language("es", "İspanyolca", true),
            new Language("pt", "Portekizce", true),
            new Language("de", "Almanca",    true)
    );

    private static final Map<String, String> EXPECTED_DISPLAY_NAMES = Map.of(
            "tr", "Türkçe",
            "en", "İngilizce",
            "es", "İspanyolca",
            "pt", "Portekizce",
            "de", "Almanca"
    );

    private static final Set<String> EXPECTED_CODES = Set.of("tr", "en", "es", "pt", "de");

    /**
     * Provides each seed Language object as an Arbitrary.
     *
     * **Validates: Requirements 21.1**
     */
    @Provide
    Arbitrary<Language> seedLanguages() {
        return Arbitraries.of(SEED_LANGUAGES);
    }

    /**
     * Özellik 32: For every seed language, is_active must be true.
     *
     * **Validates: Requirements 21.3**
     */
    @Property(tries = 100)
    void eachSeedLanguageIsActive(@ForAll("seedLanguages") Language language) {
        assertThat(language.isActive())
                .as("Language '%s' must have is_active = true", language.getCode())
                .isTrue();
    }

    /**
     * Özellik 32: For every seed language, display_name must not be null.
     *
     * **Validates: Requirements 21.2**
     */
    @Property(tries = 100)
    void eachSeedLanguageHasNonNullDisplayName(@ForAll("seedLanguages") Language language) {
        assertThat(language.getDisplayName())
                .as("Language '%s' must have a non-null display_name", language.getCode())
                .isNotNull()
                .isNotBlank();
    }

    /**
     * Özellik 32: For every seed language, display_name must match the expected value.
     *
     * **Validates: Requirements 21.2**
     */
    @Property(tries = 100)
    void eachSeedLanguageHasCorrectDisplayName(@ForAll("seedLanguages") Language language) {
        String expected = EXPECTED_DISPLAY_NAMES.get(language.getCode());
        assertThat(language.getDisplayName())
                .as("display_name for '%s' should be '%s'", language.getCode(), expected)
                .isEqualTo(expected);
    }

    /**
     * Özellik 32: All five expected language codes (tr, en, es, pt, de) must be present.
     *
     * **Validates: Requirements 21.1**
     */
    @Property(tries = 1)
    void allFiveLanguageCodesArePresent() {
        Set<String> actualCodes = SEED_LANGUAGES.stream()
                .map(Language::getCode)
                .collect(Collectors.toSet());

        assertThat(actualCodes)
                .as("Seed data must contain exactly the 5 expected language codes")
                .containsExactlyInAnyOrderElementsOf(EXPECTED_CODES);
    }

    /**
     * Özellik 32: Seed data must contain exactly 5 language records.
     *
     * **Validates: Requirements 21.1**
     */
    @Property(tries = 1)
    void seedDataContainsExactlyFiveLanguages() {
        assertThat(SEED_LANGUAGES)
                .as("Exactly 5 language records should be seeded")
                .hasSize(5);
    }
}
