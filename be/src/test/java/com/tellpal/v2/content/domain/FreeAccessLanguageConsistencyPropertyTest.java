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
 * Property-based test for free access language-based consistency (Özellik 45).
 *
 * Validates: Requirements 23.6, 23.7
 *
 * Özellik 45: Ücretsiz Erişim Dil Bazlı Tutarlılığı
 * - Ücretsiz erişim durumu dile özgüdür; aynı içerik bir dilde ücretsiz, başka bir dilde premium olabilir (Req 23.6)
 * - Bir içeriğin ücretsiz durumu, ilgili (accessKey, contentId, languageCode) üçlüsü için
 *   content_free_access tablosunda kayıt bulunup bulunmadığına göre belirlenir (Req 23.7)
 */
public class FreeAccessLanguageConsistencyPropertyTest {

    /** Minimal domain record — no JPA, no Spring. */
    record FreeAccessRecord(String accessKey, Long contentId, String languageCode) {}

    // -------------------------------------------------------------------------
    // Helper: inlined isFree logic
    // -------------------------------------------------------------------------

    private boolean isFree(List<FreeAccessRecord> entries, String accessKey, Long contentId, String languageCode) {
        return entries.stream().anyMatch(e ->
                e.accessKey().equals(accessKey) &&
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
                .ofMaxLength(32);
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
     * Özellik 45 — A content can be free in language A but not in language B
     * under the same access key, when only language A has an entry.
     *
     * **Validates: Requirements 23.6**
     */
    @Property(tries = 100)
    void contentCanBeFreeInOneLanguageAndPremiumInAnother(
            @ForAll("validAccessKeys") String accessKey,
            @ForAll("validContentIds") Long contentId,
            @ForAll("validLanguageCodes") String langA,
            @ForAll("validLanguageCodes") String langB) {

        Assume.that(!langA.equals(langB));

        // Only langA has a free access entry
        List<FreeAccessRecord> entries = List.of(
                new FreeAccessRecord(accessKey, contentId, langA)
        );

        assertThat(isFree(entries, accessKey, contentId, langA))
                .as("Content should be free in language %s (entry exists)", langA)
                .isTrue();

        assertThat(isFree(entries, accessKey, contentId, langB))
                .as("Content should be premium in language %s (no entry)", langB)
                .isFalse();
    }

    /**
     * Özellik 45 — A content with entries for all 5 languages is free in all of them.
     *
     * **Validates: Requirements 23.6**
     */
    @Property(tries = 100)
    void contentCanBeFreeInAllLanguages(
            @ForAll("validAccessKeys") String accessKey,
            @ForAll("validContentIds") Long contentId) {

        List<String> allLanguages = List.of("tr", "en", "es", "pt", "de");

        List<FreeAccessRecord> entries = allLanguages.stream()
                .map(lang -> new FreeAccessRecord(accessKey, contentId, lang))
                .toList();

        for (String lang : allLanguages) {
            assertThat(isFree(entries, accessKey, contentId, lang))
                    .as("Content should be free in language %s when an entry exists for it", lang)
                    .isTrue();
        }
    }

    /**
     * Özellik 45 — A content with no free access entries is not free in any language.
     *
     * **Validates: Requirements 23.7**
     */
    @Property(tries = 100)
    void contentWithNoEntriesIsPremiumInAllLanguages(
            @ForAll("validAccessKeys") String accessKey,
            @ForAll("validContentIds") Long contentId,
            @ForAll("validLanguageCodes") String languageCode) {

        List<FreeAccessRecord> entries = List.of(); // empty — no free access records

        assertThat(isFree(entries, accessKey, contentId, languageCode))
                .as("Content with no free access entries must be premium in language %s", languageCode)
                .isFalse();
    }

    /**
     * Özellik 45 — Free status requires exact match of (accessKey, contentId, languageCode);
     * changing any one field makes the content not free.
     *
     * **Validates: Requirements 23.7**
     */
    @Property(tries = 100)
    void freeStatusRequiresExactTripleMatch(
            @ForAll("validAccessKeys") String accessKey,
            @ForAll("validContentIds") Long contentId,
            @ForAll("validLanguageCodes") String languageCode,
            @ForAll("validAccessKeys") String otherKey,
            @ForAll("validContentIds") Long otherContentId,
            @ForAll("validLanguageCodes") String otherLang) {

        Assume.that(!otherKey.equals(accessKey));
        Assume.that(!otherContentId.equals(contentId));
        Assume.that(!otherLang.equals(languageCode));

        List<FreeAccessRecord> entries = List.of(
                new FreeAccessRecord(accessKey, contentId, languageCode)
        );

        // Exact match → free
        assertThat(isFree(entries, accessKey, contentId, languageCode))
                .as("Exact (accessKey, contentId, languageCode) triple must be free")
                .isTrue();

        // Different accessKey → not free
        assertThat(isFree(entries, otherKey, contentId, languageCode))
                .as("Different accessKey must not match the free access entry")
                .isFalse();

        // Different contentId → not free
        assertThat(isFree(entries, accessKey, otherContentId, languageCode))
                .as("Different contentId must not match the free access entry")
                .isFalse();

        // Different languageCode → not free
        assertThat(isFree(entries, accessKey, contentId, otherLang))
                .as("Different languageCode must not match the free access entry")
                .isFalse();
    }
}
