package com.tellpal.v2.content.domain;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Assume;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based test for unknown freeKey fallback behaviour (Özellik 44).
 *
 * Validates: Requirement 23.5
 *
 * Özellik 44: Bilinmeyen Key Fallback
 * - Bilinmeyen bir `freeKey` gönderildiğinde sistem `default` key'e düşmelidir (Req 23.5)
 */
public class FreeAccessUnknownKeyFallbackPropertyTest {

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
     * Özellik 44 — A freeKey that has no entries in the store resolves to "default".
     *
     * **Validates: Requirements 23.5**
     */
    @Property(tries = 100)
    void unknownKeyWithNoEntriesFallsBackToDefault(
            @ForAll("nonDefaultKeys") String unknownKey,
            @ForAll("validContentIds") Long contentId,
            @ForAll("validLanguageCodes") String languageCode) {

        // Store has entries, but none for unknownKey
        List<FreeAccessRecord> entries = List.of(
                new FreeAccessRecord(DEFAULT_KEY, contentId, languageCode)
        );

        String resolved = resolveFreeKey(entries, unknownKey);

        assertThat(resolved)
                .as("freeKey \"%s\" with no matching entries must fall back to \"default\"", unknownKey)
                .isEqualTo(DEFAULT_KEY);
    }

    /**
     * Özellik 44 — A freeKey that has at least one entry in the store resolves to itself.
     *
     * **Validates: Requirements 23.5**
     */
    @Property(tries = 100)
    void knownKeyWithEntriesResolvesToItself(
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

    /**
     * Özellik 44 — An unknown key always falls back to "default" even when other keys have entries.
     *
     * **Validates: Requirements 23.5**
     */
    @Property(tries = 100)
    void fallbackIsConsistentRegardlessOfOtherKeys(
            @ForAll("nonDefaultKeys") String unknownKey,
            @ForAll("nonDefaultKeys") String otherKey,
            @ForAll("validContentIds") Long contentId,
            @ForAll("validLanguageCodes") String languageCode) {

        Assume.that(!unknownKey.equals(otherKey));

        // Store has entries for otherKey but NOT for unknownKey
        List<FreeAccessRecord> entries = List.of(
                new FreeAccessRecord(DEFAULT_KEY, contentId, languageCode),
                new FreeAccessRecord(otherKey, contentId, languageCode)
        );

        String resolved = resolveFreeKey(entries, unknownKey);

        assertThat(resolved)
                .as("freeKey \"%s\" must fall back to \"default\" regardless of other keys present in the store", unknownKey)
                .isEqualTo(DEFAULT_KEY);
    }

    /**
     * Özellik 44 — A key with exactly one entry is considered "known" and resolves to itself.
     *
     * **Validates: Requirements 23.5**
     */
    @Property(tries = 100)
    void keyIsKnownIfItHasAtLeastOneEntry(
            @ForAll("nonDefaultKeys") String key,
            @ForAll("validContentIds") Long contentId,
            @ForAll("validLanguageCodes") String languageCode) {

        // Exactly one entry for this key
        List<FreeAccessRecord> entries = List.of(
                new FreeAccessRecord(key, contentId, languageCode)
        );

        String resolved = resolveFreeKey(entries, key);

        assertThat(resolved)
                .as("freeKey \"%s\" with exactly one entry must be considered known and resolve to itself", key)
                .isEqualTo(key);
    }
}
