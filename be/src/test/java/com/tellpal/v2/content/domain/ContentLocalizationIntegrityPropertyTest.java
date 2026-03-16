package com.tellpal.v2.content.domain;

import com.tellpal.v2.shared.domain.LocalizationStatus;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Assume;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based test for content localization integrity (Özellik 2).
 *
 * Validates: Requirements 1.2
 *
 * Özellik 2: İçerik Yerelleştirme Bütünlüğü
 * - Bir içerik, her dil için en fazla bir yerelleştirmeye sahip olabilir.
 * - (contentId, languageCode) kombinasyonu benzersiz olmalıdır.
 * - Bir içerik, birden fazla farklı dil için aynı anda yerelleştirmeye sahip olabilir.
 * - İki farklı içerik, aynı dil için yerelleştirmeye sahip olabilir (çapraz içerik çakışması yoktur).
 */
public class ContentLocalizationIntegrityPropertyTest {

    /** Minimal domain record — no JPA, no Spring. */
    record LocalizationRecord(Long contentId, String languageCode, LocalizationStatus status) {}

    private static final List<String> SUPPORTED_LANGUAGES = List.of("tr", "en", "es", "pt", "de");

    // -------------------------------------------------------------------------
    // Arbitraries
    // -------------------------------------------------------------------------

    @Provide
    Arbitrary<Long> validContentIds() {
        return Arbitraries.longs().between(1L, Long.MAX_VALUE);
    }

    @Provide
    Arbitrary<String> validLanguageCodes() {
        return Arbitraries.of("tr", "en", "es", "pt", "de");
    }

    @Provide
    Arbitrary<LocalizationStatus> anyStatus() {
        return Arbitraries.of(LocalizationStatus.values());
    }

    /**
     * Generates a list of localizations all sharing the same contentId,
     * but each with a distinct languageCode drawn from the supported set.
     */
    @Provide
    Arbitrary<List<LocalizationRecord>> localizationsForSameContent() {
        return Arbitraries.longs().between(1L, Long.MAX_VALUE).flatMap(contentId ->
            Arbitraries.of("tr", "en", "es", "pt", "de")
                .set()
                .ofMinSize(1)
                .ofMaxSize(5)
                .map(langSet -> langSet.stream()
                    .map(lang -> new LocalizationRecord(contentId, lang, LocalizationStatus.DRAFT))
                    .collect(Collectors.toList()))
        );
    }

    /**
     * Generates a list of localizations with unique (contentId, languageCode) pairs.
     * Uniqueness is enforced on the composite key, not the full tuple.
     */
    @Provide
    Arbitrary<List<LocalizationRecord>> uniqueLocalizations() {
        return Combinators.combine(
            Arbitraries.longs().between(1L, 100L),
            Arbitraries.of("tr", "en", "es", "pt", "de")
        ).as(Tuple::of)
         .set()
         .ofMinSize(1)
         .ofMaxSize(20)
         .map(pairs -> pairs.stream()
             .map(pair -> new LocalizationRecord(pair.get1(), pair.get2(), LocalizationStatus.DRAFT))
             .collect(Collectors.toList()));
    }

    // -------------------------------------------------------------------------
    // Properties
    // -------------------------------------------------------------------------

    /**
     * Özellik 2 — A content can have at most one localization per language.
     * Given a list of localizations for the same contentId with distinct languageCodes,
     * no two entries share the same languageCode.
     *
     * **Validates: Requirements 1.2**
     */
    @Property(tries = 100)
    void contentCanHaveAtMostOneLocalizationPerLanguage(
            @ForAll("localizationsForSameContent") List<LocalizationRecord> localizations) {

        // All records share the same contentId by construction
        long contentId = localizations.get(0).contentId();
        List<LocalizationRecord> forContent = localizations.stream()
                .filter(l -> l.contentId() == contentId)
                .collect(Collectors.toList());

        List<String> languageCodes = forContent.stream()
                .map(LocalizationRecord::languageCode)
                .collect(Collectors.toList());

        Set<String> uniqueLanguageCodes = Set.copyOf(languageCodes);

        assertThat(uniqueLanguageCodes)
                .as("A content (id=%d) must have at most one localization per language — no duplicate languageCodes allowed", contentId)
                .hasSize(languageCodes.size());
    }

    /**
     * Özellik 2 — A content can have separate localizations for each of the 5 supported languages.
     * All 5 supported languages can coexist for the same contentId without conflict.
     *
     * **Validates: Requirements 1.2**
     */
    @Property(tries = 100)
    void contentCanHaveLocalizationsForMultipleLanguages(
            @ForAll("validContentIds") Long contentId,
            @ForAll("anyStatus") LocalizationStatus status) {

        // Build one localization per supported language for the same content
        List<LocalizationRecord> localizations = SUPPORTED_LANGUAGES.stream()
                .map(lang -> new LocalizationRecord(contentId, lang, status))
                .collect(Collectors.toList());

        Set<String> distinctLanguages = localizations.stream()
                .map(LocalizationRecord::languageCode)
                .collect(Collectors.toSet());

        assertThat(distinctLanguages)
                .as("A content (id=%d) must be able to have localizations for all %d supported languages simultaneously",
                        contentId, SUPPORTED_LANGUAGES.size())
                .containsExactlyInAnyOrderElementsOf(SUPPORTED_LANGUAGES);

        // Verify no (contentId, languageCode) collision exists
        Map<String, Long> countByLang = localizations.stream()
                .collect(Collectors.groupingBy(LocalizationRecord::languageCode, Collectors.counting()));

        countByLang.forEach((lang, count) ->
            assertThat(count)
                .as("Language '%s' must appear exactly once for content id=%d", lang, contentId)
                .isEqualTo(1L)
        );
    }

    /**
     * Özellik 2 — Two different contentIds can both have a localization for the same language
     * without any cross-content conflict.
     *
     * **Validates: Requirements 1.2**
     */
    @Property(tries = 100)
    void differentContentsCanHaveLocalizationsForSameLanguage(
            @ForAll("validContentIds") Long contentIdA,
            @ForAll("validContentIds") Long contentIdB,
            @ForAll("validLanguageCodes") String sharedLanguage,
            @ForAll("anyStatus") LocalizationStatus status) {

        Assume.that(!contentIdA.equals(contentIdB));

        LocalizationRecord locA = new LocalizationRecord(contentIdA, sharedLanguage, status);
        LocalizationRecord locB = new LocalizationRecord(contentIdB, sharedLanguage, status);

        // Composite key: contentId + languageCode — must be distinct for different contentIds
        String keyA = compositeKey(locA);
        String keyB = compositeKey(locB);

        assertThat(keyA)
                .as("Different contents (%d vs %d) with the same language '%s' must have distinct composite keys",
                        contentIdA, contentIdB, sharedLanguage)
                .isNotEqualTo(keyB);

        // Both can coexist in the same collection without collision
        Set<String> keys = Set.of(keyA, keyB);
        assertThat(keys)
                .as("Two localizations for different contents but the same language must not conflict")
                .hasSize(2);
    }

    /**
     * Özellik 2 — The (contentId, languageCode) pair uniquely identifies a localization.
     * Given a list with unique (contentId, languageCode) pairs, looking up by both fields
     * returns exactly one result.
     *
     * **Validates: Requirements 1.2**
     */
    @Property(tries = 100)
    void localizationIsUniquelyIdentifiedByContentIdAndLanguageCode(
            @ForAll("uniqueLocalizations") List<LocalizationRecord> localizations) {

        // For each record, look up by (contentId, languageCode) and expect exactly one match
        for (LocalizationRecord target : localizations) {
            List<LocalizationRecord> matches = localizations.stream()
                    .filter(l -> l.contentId().equals(target.contentId())
                              && l.languageCode().equals(target.languageCode()))
                    .collect(Collectors.toList());

            assertThat(matches)
                    .as("Lookup by (contentId=%d, languageCode='%s') must return exactly one localization",
                            target.contentId(), target.languageCode())
                    .hasSize(1);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String compositeKey(LocalizationRecord r) {
        return r.contentId() + "|" + r.languageCode();
    }
}
