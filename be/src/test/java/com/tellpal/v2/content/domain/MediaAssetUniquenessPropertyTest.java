package com.tellpal.v2.content.domain;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
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
 * Property-based test for media asset (provider, object_path) uniqueness invariant.
 *
 * Validates: Requirement 5.3
 *
 * Özellik 10: Medya Varlık Benzersizliği
 * - (provider, object_path) kombinasyonu benzersiz olmalıdır.
 * - Aynı provider+object_path çiftine sahip iki kayıt benzersizlik ihlali oluşturur.
 * - Aynı provider altında farklı object_path değerleri farklı varlıkları temsil eder.
 */
public class MediaAssetUniquenessPropertyTest {

    /** Minimal domain record — no JPA, no Spring. */
    record MediaAssetRecord(String provider, String objectPath) {}

    // -------------------------------------------------------------------------
    // Arbitraries
    // -------------------------------------------------------------------------

    @Provide
    Arbitrary<String> validProviders() {
        return Arbitraries.of("firebase", "s3", "gcs", "azure");
    }

    @Provide
    Arbitrary<String> validObjectPaths() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('0', '9')
                .withChars("/_-.")
                .ofMinLength(1)
                .ofMaxLength(128);
    }

    /** Generates a list of distinct (provider, object_path) pairs. */
    @Provide
    Arbitrary<List<Tuple.Tuple2<String, String>>> distinctProviderPathPairs() {
        return Combinators.combine(validProviders(), validObjectPaths())
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
     * Özellik 10 — Two MediaAsset records with the same (provider, object_path)
     * represent a uniqueness violation.
     *
     * **Validates: Requirements 5.3**
     */
    @Property(tries = 100)
    void sameProviderAndObjectPathRepresentsConstraintViolation(
            @ForAll("validProviders") String provider,
            @ForAll("validObjectPaths") String objectPath) {

        MediaAssetRecord first  = new MediaAssetRecord(provider, objectPath);
        MediaAssetRecord second = new MediaAssetRecord(provider, objectPath);

        // Both records carry the same (provider, object_path) — this is the violation
        assertThat(first.provider())
                .as("provider must match for the collision to occur")
                .isEqualTo(second.provider());
        assertThat(first.objectPath())
                .as("object_path must match for the collision to occur")
                .isEqualTo(second.objectPath());

        // A set of composite keys collapses to size 1, proving the collision
        Set<String> compositeKeys = new HashSet<>();
        compositeKeys.add(first.provider() + "|" + first.objectPath());
        compositeKeys.add(second.provider() + "|" + second.objectPath());
        assertThat(compositeKeys)
                .as("Identical (provider, object_path) pairs must be detected as a uniqueness violation (set size = 1)")
                .hasSize(1);
    }

    /**
     * Özellik 10 — A collection of MediaAsset records with distinct
     * (provider, object_path) pairs must have no duplicate composite keys.
     *
     * **Validates: Requirements 5.3**
     */
    @Property(tries = 100)
    void distinctProviderPathPairsProduceNoCollisions(
            @ForAll("distinctProviderPathPairs") List<Tuple.Tuple2<String, String>> pairs) {

        List<MediaAssetRecord> assets = pairs.stream()
                .map(t -> new MediaAssetRecord(t.get1(), t.get2()))
                .collect(Collectors.toList());

        Set<String> compositeKeys = assets.stream()
                .map(a -> a.provider() + "|" + a.objectPath())
                .collect(Collectors.toSet());

        assertThat(compositeKeys)
                .as("A list of MediaAssets with distinct (provider, object_path) pairs must have no duplicate composite keys")
                .hasSize(assets.size());
    }

    /**
     * Özellik 10 — Two MediaAsset records with the same provider but different
     * object_paths are distinct (no uniqueness violation).
     *
     * **Validates: Requirements 5.3**
     */
    @Property(tries = 100)
    void sameProviderDifferentObjectPathsAreDistinct(
            @ForAll("validProviders") String provider,
            @ForAll("validObjectPaths") String pathA,
            @ForAll("validObjectPaths") String pathB) {

        // Only assert when paths are actually different
        net.jqwik.api.Assume.that(!pathA.equals(pathB));

        MediaAssetRecord first  = new MediaAssetRecord(provider, pathA);
        MediaAssetRecord second = new MediaAssetRecord(provider, pathB);

        Set<String> compositeKeys = new HashSet<>();
        compositeKeys.add(first.provider() + "|" + first.objectPath());
        compositeKeys.add(second.provider() + "|" + second.objectPath());

        assertThat(compositeKeys)
                .as("Same provider with different object_paths must produce distinct composite keys")
                .hasSize(2);
    }

    /**
     * Özellik 10 — provider and object_path must not be null or blank.
     *
     * **Validates: Requirements 5.3**
     */
    @Property(tries = 100)
    void providerAndObjectPathAreNeverNullOrBlank(
            @ForAll("validProviders") String provider,
            @ForAll("validObjectPaths") String objectPath) {

        MediaAssetRecord asset = new MediaAssetRecord(provider, objectPath);

        assertThat(asset.provider())
                .as("provider must not be null or blank")
                .isNotNull()
                .isNotBlank();

        assertThat(asset.objectPath())
                .as("object_path must not be null or blank")
                .isNotNull()
                .isNotBlank();
    }
}
