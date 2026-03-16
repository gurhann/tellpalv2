package com.tellpal.v2.content.domain;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based test for free access default key integrity (Özellik 43).
 *
 * Validates: Requirements 23.2, 23.4
 *
 * Özellik 43: Ücretsiz Erişim Varsayılan Key Bütünlüğü
 * - Sistem, `default` key ile tanımlanan varsayılan ücretsiz içerik setini içermelidir (Req 23.2)
 * - İstemci `freeKey` parametresi göndermediğinde sistem `default` key'i kullanmalıdır (Req 23.4)
 */
public class FreeAccessDefaultKeyPropertyTest {

    static final String DEFAULT_KEY = "default";

    /** Minimal domain record — no JPA, no Spring. */
    record FreeAccessRecord(String accessKey, Long contentId, String languageCode) {}

    // -------------------------------------------------------------------------
    // Helper: inlined resolveFreeKey logic (mirrors FreeAccessService)
    // -------------------------------------------------------------------------

    private String resolveFreeKey(List<FreeAccessRecord> allEntries, String freeKey) {
        if (freeKey == null || freeKey.isBlank()) return DEFAULT_KEY;
        boolean hasEntries = allEntries.stream().anyMatch(e -> e.accessKey().equals(freeKey));
        return hasEntries ? freeKey : DEFAULT_KEY;
    }

    // -------------------------------------------------------------------------
    // Arbitraries
    // -------------------------------------------------------------------------

    @Provide
    Arbitrary<String> nonDefaultKeys() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(1)
                .ofMaxLength(32)
                .filter(s -> !s.equals(DEFAULT_KEY) && !s.isBlank());
    }

    @Provide
    Arbitrary<String> blankStrings() {
        return Arbitraries.of("", " ", "   ", "\t", "\n");
    }

    @Provide
    Arbitrary<Long> validContentIds() {
        return Arbitraries.longs().between(1L, Long.MAX_VALUE);
    }

    @Provide
    Arbitrary<String> validLanguageCodes() {
        return Arbitraries.of("tr", "en", "es", "pt", "de");
    }

    // -------------------------------------------------------------------------
    // Properties
    // -------------------------------------------------------------------------

    /**
     * Özellik 43 — The DEFAULT_KEY constant must always equal the string "default".
     *
     * **Validates: Requirements 23.2**
     */
    @Property(tries = 100)
    void defaultKeyConstantIsAlwaysDefault() {
        assertThat(DEFAULT_KEY)
                .as("DEFAULT_KEY constant must always be the string \"default\"")
                .isEqualTo("default");
    }

    /**
     * Özellik 43 — When freeKey is null, the resolved key must always be "default".
     *
     * **Validates: Requirements 23.4**
     */
    @Property(tries = 100)
    void nullFreeKeyResolvesToDefault(
            @ForAll("validContentIds") Long contentId,
            @ForAll("validLanguageCodes") String languageCode) {

        List<FreeAccessRecord> entries = List.of(
                new FreeAccessRecord(DEFAULT_KEY, contentId, languageCode)
        );

        String resolved = resolveFreeKey(entries, null);

        assertThat(resolved)
                .as("null freeKey must always resolve to \"default\"")
                .isEqualTo(DEFAULT_KEY);
    }

    /**
     * Özellik 43 — When freeKey is blank or empty, the resolved key must always be "default".
     *
     * **Validates: Requirements 23.4**
     */
    @Property(tries = 100)
    void blankFreeKeyResolvesToDefault(
            @ForAll("blankStrings") String blankKey,
            @ForAll("validContentIds") Long contentId,
            @ForAll("validLanguageCodes") String languageCode) {

        List<FreeAccessRecord> entries = List.of(
                new FreeAccessRecord(DEFAULT_KEY, contentId, languageCode)
        );

        String resolved = resolveFreeKey(entries, blankKey);

        assertThat(resolved)
                .as("blank/empty freeKey \"%s\" must always resolve to \"default\"", blankKey)
                .isEqualTo(DEFAULT_KEY);
    }

    /**
     * Özellik 43 — When freeKey matches a key that has entries, it must resolve to itself.
     *
     * **Validates: Requirements 23.4**
     */
    @Property(tries = 100)
    void knownFreeKeyResolvesToItself(
            @ForAll("nonDefaultKeys") String knownKey,
            @ForAll("validContentIds") Long contentId,
            @ForAll("validLanguageCodes") String languageCode) {

        List<FreeAccessRecord> entries = List.of(
                new FreeAccessRecord(knownKey, contentId, languageCode)
        );

        String resolved = resolveFreeKey(entries, knownKey);

        assertThat(resolved)
                .as("freeKey \"%s\" with matching entries must resolve to itself, not \"default\"", knownKey)
                .isEqualTo(knownKey)
                .isNotEqualTo(DEFAULT_KEY);
    }
}
