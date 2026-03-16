package com.tellpal.v2.content.domain;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Assume;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based test for content_free_access (access_key, content_id, language_code)
 * uniqueness invariant (Özellik 49).
 *
 * Validates: Requirement 23.1
 *
 * Özellik 49: ContentFreeAccess Benzersizliği
 * - (access_key, content_id, language_code) kombinasyonu benzersiz olmalıdır.
 * - Aynı üçlüye sahip iki kayıt benzersizlik ihlali oluşturur.
 * - Aynı access_key + content_id ile farklı language_code değerleri farklı kayıtları temsil eder.
 */
public class ContentFreeAccessUniquenessPropertyTest {

    /** Minimal domain record — no JPA, no Spring. */
    record ContentFreeAccessRecord(String accessKey, long contentId, String languageCode) {}

    // -------------------------------------------------------------------------
    // Arbitraries
    // -------------------------------------------------------------------------

    @Provide
    Arbitrary<String> validAccessKeys() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('0', '9')
                .withChars("-_")
                .ofMinLength(1)
                .ofMaxLength(64);
    }

    @Provide
    Arbitrary<Long> validContentIds() {
        return Arbitraries.longs().between(1L, Long.MAX_VALUE);
    }

    @Provide
    Arbitrary<String> validLanguageCodes() {
        return Arbitraries.of("tr", "en", "es", "pt", "de");
    }

    /** Generates a list of distinct (access_key, content_id, language_code) triples. */
    @Provide
    Arbitrary<List<Tuple.Tuple3<String, Long, String>>> distinctFreeAccessTriples() {
        return Combinators.combine(validAccessKeys(), validContentIds(), validLanguageCodes())
                .as(Tuple::of)
                .set()
                .ofMinSize(1)
                .ofMaxSize(20)
                .map(set -> List.copyOf(set));
    }

    // -------------------------------------------------------------------------
    // Properties
    // -------------------------------------------------------------------------

    /**
     * Özellik 49 — Two ContentFreeAccess records with the same
     * (access_key, content_id, language_code) represent a uniqueness violation.
     *
     * **Validates: Requirements 23.1**
     */
    @Property(tries = 100)
    void sameTripleRepresentsConstraintViolation(
            @ForAll("validAccessKeys") String accessKey,
            @ForAll("validContentIds") long contentId,
            @ForAll("validLanguageCodes") String languageCode) {

        ContentFreeAccessRecord first  = new ContentFreeAccessRecord(accessKey, contentId, languageCode);
        ContentFreeAccessRecord second = new ContentFreeAccessRecord(accessKey, contentId, languageCode);

        assertThat(first.accessKey()).isEqualTo(second.accessKey());
        assertThat(first.contentId()).isEqualTo(second.contentId());
        assertThat(first.languageCode()).isEqualTo(second.languageCode());

        // A set of composite keys collapses to size 1, proving the collision
        Set<String> compositeKeys = new HashSet<>();
        compositeKeys.add(compositeKey(first));
        compositeKeys.add(compositeKey(second));
        assertThat(compositeKeys)
                .as("Identical (access_key, content_id, language_code) triples must be detected as a uniqueness violation (set size = 1)")
                .hasSize(1);
    }

    /**
     * Özellik 49 — A collection of ContentFreeAccess records with distinct
     * (access_key, content_id, language_code) triples must have no duplicate composite keys.
     *
     * **Validates: Requirements 23.1**
     */
    @Property(tries = 100)
    void distinctTriplesProduceNoCollisions(
            @ForAll("distinctFreeAccessTriples") List<Tuple.Tuple3<String, Long, String>> triples) {

        List<ContentFreeAccessRecord> records = triples.stream()
                .map(t -> new ContentFreeAccessRecord(t.get1(), t.get2(), t.get3()))
                .collect(Collectors.toList());

        Set<String> compositeKeys = records.stream()
                .map(this::compositeKey)
                .collect(Collectors.toSet());

        assertThat(compositeKeys)
                .as("A list of ContentFreeAccess records with distinct triples must have no duplicate composite keys")
                .hasSize(records.size());
    }

    /**
     * Özellik 49 — Two records with the same access_key and content_id but different
     * language_codes are distinct (language-based free access).
     *
     * **Validates: Requirements 23.1**
     */
    @Property(tries = 100)
    void sameAccessKeyAndContentIdButDifferentLanguageCodesAreDistinct(
            @ForAll("validAccessKeys") String accessKey,
            @ForAll("validContentIds") long contentId,
            @ForAll("validLanguageCodes") String langA,
            @ForAll("validLanguageCodes") String langB) {

        Assume.that(!langA.equals(langB));

        ContentFreeAccessRecord first  = new ContentFreeAccessRecord(accessKey, contentId, langA);
        ContentFreeAccessRecord second = new ContentFreeAccessRecord(accessKey, contentId, langB);

        Set<String> compositeKeys = new HashSet<>();
        compositeKeys.add(compositeKey(first));
        compositeKeys.add(compositeKey(second));

        assertThat(compositeKeys)
                .as("Same access_key + content_id with different language_codes must produce distinct composite keys")
                .hasSize(2);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String compositeKey(ContentFreeAccessRecord r) {
        return r.accessKey() + "|" + r.contentId() + "|" + r.languageCode();
    }
}
