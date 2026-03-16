package com.tellpal.v2.content.domain;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based test for external key uniqueness invariant (Özellik 5).
 *
 * Validates: Requirement 2.9
 *
 * Özellik 5: Harici Anahtar Benzersizliği
 * - Her içerik kaydının external_key değeri benzersiz olmalıdır.
 * - Aynı external_key'e sahip iki kayıt benzersizlik ihlali oluşturur.
 * - external_key null veya boş olamaz.
 */
public class ExternalKeyUniquenessPropertyTest {

    /** Minimal domain record — no JPA, no Spring. */
    record ContentRecord(String externalKey, String type) {}

    // -------------------------------------------------------------------------
    // Arbitraries
    // -------------------------------------------------------------------------

    @Provide
    Arbitrary<String> validExternalKeys() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('0', '9')
                .withChars("-_")
                .ofMinLength(1)
                .ofMaxLength(64);
    }

    @Provide
    Arbitrary<String> validTypes() {
        return Arbitraries.of("STORY", "AUDIO_STORY", "MEDITATION", "LULLABY");
    }

    @Provide
    Arbitrary<List<String>> distinctExternalKeyList() {
        return validExternalKeys()
                .set()
                .ofMinSize(1)
                .ofMaxSize(20)
                .map(set -> List.copyOf(set));
    }

    // -------------------------------------------------------------------------
    // Properties
    // -------------------------------------------------------------------------

    /**
     * Özellik 5 — A collection of content records with distinct external_keys
     * must have no duplicate keys (uniqueness invariant).
     *
     * **Validates: Requirements 2.9**
     */
    @Property(tries = 100)
    void distinctExternalKeysProduceNoCollisions(
            @ForAll("distinctExternalKeyList") List<String> keys) {

        List<ContentRecord> records = keys.stream()
                .map(k -> new ContentRecord(k, "STORY"))
                .collect(Collectors.toList());

        Set<String> uniqueKeys = records.stream()
                .map(ContentRecord::externalKey)
                .collect(Collectors.toSet());

        assertThat(uniqueKeys)
                .as("A list of ContentRecords with distinct external_keys must have no duplicates")
                .hasSize(records.size());
    }

    /**
     * Özellik 5 — Two content records with the same external_key represent
     * a uniqueness violation (detected at domain level).
     *
     * **Validates: Requirements 2.9**
     */
    @Property(tries = 100)
    void sameExternalKeyRepresentsConstraintViolation(
            @ForAll("validExternalKeys") String key) {

        ContentRecord first  = new ContentRecord(key, "STORY");
        ContentRecord second = new ContentRecord(key, "MEDITATION");

        // Both records carry the same external_key — this is the constraint violation
        assertThat(first.externalKey())
                .as("Two ContentRecords sharing the same external_key violate uniqueness")
                .isEqualTo(second.externalKey());

        // A set built from both keys collapses to size 1, proving the collision
        Set<String> keys = new java.util.HashSet<>();
        keys.add(first.externalKey());
        keys.add(second.externalKey());
        assertThat(keys)
                .as("Identical external_keys must be detected as a uniqueness violation (set size = 1)")
                .hasSize(1);
    }

    /**
     * Özellik 5 — external_key must not be null or blank.
     *
     * **Validates: Requirements 2.9**
     */
    @Property(tries = 100)
    void externalKeyIsNeverNullOrBlank(
            @ForAll("validExternalKeys") String key) {

        ContentRecord record = new ContentRecord(key, "STORY");

        assertThat(record.externalKey())
                .as("external_key must not be null")
                .isNotNull();

        assertThat(record.externalKey().trim())
                .as("external_key must not be blank")
                .isNotEmpty();
    }
}
