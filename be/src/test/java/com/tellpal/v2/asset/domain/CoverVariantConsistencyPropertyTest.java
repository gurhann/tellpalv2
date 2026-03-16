package com.tellpal.v2.asset.domain;

import com.tellpal.v2.asset.application.ImageVariant;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based test for cover image variant consistency (Özellik 40).
 *
 * **Validates: Requirements 22.6**
 *
 * Özellik 40: Kapak Varyantları Tutarlılığı
 * - Image optimization must produce exactly 4 cover variants:
 *   THUMBNAIL_PHONE, THUMBNAIL_TABLET, DETAIL_PHONE, DETAIL_TABLET.
 * - Each variant has a unique suffix.
 * - All 4 variants are always produced (no partial sets).
 */
public class CoverVariantConsistencyPropertyTest {

    private static final Set<String> EXPECTED_VARIANTS = Set.of(
            "THUMBNAIL_PHONE", "THUMBNAIL_TABLET", "DETAIL_PHONE", "DETAIL_TABLET"
    );

    private static final int EXPECTED_VARIANT_COUNT = 4;

    // -------------------------------------------------------------------------
    // Arbitraries
    // -------------------------------------------------------------------------

    @Provide
    Arbitrary<ImageVariant> anyVariant() {
        return Arbitraries.of(ImageVariant.values());
    }

    // -------------------------------------------------------------------------
    // Properties
    // -------------------------------------------------------------------------

    /**
     * Özellik 40 — Exactly 4 cover variants are defined.
     * Validates: Requirement 22.6
     */
    @Property(tries = 100)
    void exactlyFourCoverVariantsAreDefined() {
        assertThat(ImageVariant.values())
                .as("Must have exactly 4 cover image variants")
                .hasSize(EXPECTED_VARIANT_COUNT);
    }

    /**
     * Özellik 40 — All expected variant names are present.
     * Validates: Requirement 22.6
     */
    @Property(tries = 100)
    void allExpectedVariantNamesArePresent() {
        Set<String> actualNames = Arrays.stream(ImageVariant.values())
                .map(Enum::name)
                .collect(Collectors.toSet());

        assertThat(actualNames)
                .as("All 4 expected cover variants must be defined")
                .containsExactlyInAnyOrderElementsOf(EXPECTED_VARIANTS);
    }

    /**
     * Özellik 40 — Each variant has a non-blank suffix.
     * Validates: Requirement 22.6
     */
    @Property(tries = 100)
    void eachVariantHasNonBlankSuffix(@ForAll("anyVariant") ImageVariant variant) {
        assertThat(variant.suffix())
                .as("Variant %s must have a non-blank suffix", variant)
                .isNotBlank();
    }

    /**
     * Özellik 40 — All variant suffixes are unique.
     * Validates: Requirement 22.6
     */
    @Property(tries = 100)
    void allVariantSuffixesAreUnique() {
        List<String> suffixes = Arrays.stream(ImageVariant.values())
                .map(ImageVariant::suffix)
                .toList();

        Set<String> uniqueSuffixes = Set.copyOf(suffixes);

        assertThat(uniqueSuffixes)
                .as("All cover variant suffixes must be unique")
                .hasSize(EXPECTED_VARIANT_COUNT);
    }

    /**
     * Özellik 40 — Each variant suffix is the lowercase version of its name.
     * Validates: Requirement 22.6
     */
    @Property(tries = 100)
    void variantSuffixIsLowercaseOfName(@ForAll("anyVariant") ImageVariant variant) {
        assertThat(variant.suffix())
                .as("Variant %s suffix must be the lowercase of its name", variant)
                .isEqualTo(variant.name().toLowerCase());
    }

    /**
     * Özellik 40 — THUMBNAIL variants are present (phone and tablet).
     * Validates: Requirement 22.6
     */
    @Property(tries = 100)
    void thumbnailVariantsArePresentForBothDevices() {
        Set<String> names = Arrays.stream(ImageVariant.values())
                .map(Enum::name)
                .collect(Collectors.toSet());

        assertThat(names).contains("THUMBNAIL_PHONE");
        assertThat(names).contains("THUMBNAIL_TABLET");
    }

    /**
     * Özellik 40 — DETAIL variants are present (phone and tablet).
     * Validates: Requirement 22.6
     */
    @Property(tries = 100)
    void detailVariantsArePresentForBothDevices() {
        Set<String> names = Arrays.stream(ImageVariant.values())
                .map(Enum::name)
                .collect(Collectors.toSet());

        assertThat(names).contains("DETAIL_PHONE");
        assertThat(names).contains("DETAIL_TABLET");
    }
}
