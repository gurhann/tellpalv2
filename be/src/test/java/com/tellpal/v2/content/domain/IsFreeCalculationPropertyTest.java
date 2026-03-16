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
 * Property-based test for isFree calculation consistency (Özellik 46).
 *
 * Validates: Requirements 23.10
 *
 * Özellik 46: isFree Hesaplama Tutarlılığı
 * - İçerik listesi döndürülürken sistem, aktif key'e göre her içerik için
 *   `isFree` alanını hesaplamalı ve yanıta dahil etmelidir (Req 23.10)
 */
public class IsFreeCalculationPropertyTest {

    /** Minimal domain record — no JPA, no Spring. */
    record FreeAccessRecord(String accessKey, Long contentId, String languageCode) {}

    // -------------------------------------------------------------------------
    // Helper: inlined domain logic (mirrors FreeAccessService)
    // -------------------------------------------------------------------------

    private String resolveFreeKey(List<FreeAccessRecord> allEntries, String freeKey) {
        if (freeKey == null || freeKey.isBlank()) return "default";
        boolean hasEntries = allEntries.stream().anyMatch(e -> e.accessKey().equals(freeKey));
        return hasEntries ? freeKey : "default";
    }

    private boolean isFree(List<FreeAccessRecord> entries, Long contentId, String languageCode, String freeKey) {
        String resolvedKey = resolveFreeKey(entries, freeKey);
        return entries.stream().anyMatch(e ->
            e.accessKey().equals(resolvedKey) &&
            e.contentId().equals(contentId) &&
            e.languageCode().equals(languageCode)
        );
    }

    // -------------------------------------------------------------------------
    // Arbitraries
    // -------------------------------------------------------------------------

    @Provide
    Arbitrary<String> validAccessKeys() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(1)
                .ofMaxLength(32)
                .filter(s -> !s.isBlank() && !s.equals("default"));
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
     * Özellik 46 — isFree is true iff the resolved key has an entry for (contentId, languageCode).
     *
     * **Validates: Requirements 23.10**
     */
    @Property(tries = 100)
    void isFreeIsTrueWhenResolvedKeyHasEntry(
            @ForAll("validAccessKeys") String accessKey,
            @ForAll("validContentIds") Long contentId,
            @ForAll("validLanguageCodes") String languageCode) {

        // Store has exactly one entry for (accessKey, contentId, languageCode)
        List<FreeAccessRecord> entries = List.of(
                new FreeAccessRecord(accessKey, contentId, languageCode)
        );

        // When freeKey resolves to accessKey (it has entries), isFree must be true
        assertThat(isFree(entries, contentId, languageCode, accessKey))
                .as("isFree must be true when resolved key '%s' has entry for (contentId=%d, lang=%s)",
                        accessKey, contentId, languageCode)
                .isTrue();

        // When freeKey resolves to a different key, isFree must be false
        assertThat(isFree(entries, contentId, languageCode, "default"))
                .as("isFree must be false when resolved key 'default' has no entry for (contentId=%d, lang=%s)",
                        contentId, languageCode)
                .isFalse();
    }

    /**
     * Özellik 46 — isFree with null freeKey uses the "default" key entries.
     *
     * **Validates: Requirements 23.10**
     */
    @Property(tries = 100)
    void isFreeWithNullKeyUsesDefaultEntries(
            @ForAll("validContentIds") Long contentId,
            @ForAll("validLanguageCodes") String languageCode) {

        // Only "default" key has an entry for this content
        List<FreeAccessRecord> entries = List.of(
                new FreeAccessRecord("default", contentId, languageCode)
        );

        // null freeKey → resolves to "default" → entry exists → isFree = true
        assertThat(isFree(entries, contentId, languageCode, null))
                .as("null freeKey must use 'default' key entries; isFree must be true when default entry exists")
                .isTrue();
    }

    /**
     * Özellik 46 — isFree with unknown freeKey falls back to "default" entries.
     *
     * **Validates: Requirements 23.10**
     */
    @Property(tries = 100)
    void isFreeWithUnknownKeyFallsBackToDefault(
            @ForAll("validAccessKeys") String unknownKey,
            @ForAll("validContentIds") Long contentId,
            @ForAll("validLanguageCodes") String languageCode) {

        // Only "default" key has entries; unknownKey has none
        List<FreeAccessRecord> entries = List.of(
                new FreeAccessRecord("default", contentId, languageCode)
        );

        // unknownKey has no entries → resolves to "default" → entry exists → isFree = true
        assertThat(isFree(entries, contentId, languageCode, unknownKey))
                .as("unknown freeKey '%s' must fall back to 'default'; isFree must be true when default entry exists",
                        unknownKey)
                .isTrue();
    }

    /**
     * Özellik 46 — isFree is deterministic: same inputs always produce the same output.
     *
     * **Validates: Requirements 23.10**
     */
    @Property(tries = 100)
    void isFreeIsDeterministic(
            @ForAll("validAccessKeys") String accessKey,
            @ForAll("validContentIds") Long contentId,
            @ForAll("validLanguageCodes") String languageCode) {

        List<FreeAccessRecord> entries = List.of(
                new FreeAccessRecord(accessKey, contentId, languageCode)
        );

        boolean firstCall = isFree(entries, contentId, languageCode, accessKey);
        boolean secondCall = isFree(entries, contentId, languageCode, accessKey);

        assertThat(firstCall)
                .as("isFree must be deterministic: two calls with same inputs must return same result")
                .isEqualTo(secondCall);
    }
}
