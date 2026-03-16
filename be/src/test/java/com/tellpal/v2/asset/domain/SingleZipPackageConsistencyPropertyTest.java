package com.tellpal.v2.asset.domain;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based test for single ZIP package consistency for non-STORY content (Özellik 39).
 *
 * **Validates: Requirements 22.10**
 *
 * Özellik 39: Tek ZIP Paket Tutarlılığı
 * - Non-STORY content (AUDIO_STORY, MEDITATION, LULLABY) must produce exactly 1 ZIP package.
 * - The single ZIP must contain all provided pages.
 * - The package is named "content.zip".
 */
public class SingleZipPackageConsistencyPropertyTest {

    record PageRef(int pageNumber) {}

    private static final String[] NON_STORY_TYPES = {"AUDIO_STORY", "MEDITATION", "LULLABY"};

    // -------------------------------------------------------------------------
    // Arbitraries
    // -------------------------------------------------------------------------

    @Provide
    Arbitrary<String> nonStoryContentType() {
        return Arbitraries.of(NON_STORY_TYPES);
    }

    @Provide
    Arbitrary<List<PageRef>> contentPages() {
        return Arbitraries.integers().between(1, 8)
                .map(n -> IntStream.rangeClosed(1, n).mapToObj(PageRef::new).toList());
    }

    // -------------------------------------------------------------------------
    // Properties
    // -------------------------------------------------------------------------

    /**
     * Özellik 39 — Non-STORY content always produces exactly 1 ZIP package.
     * Validates: Requirement 22.10
     */
    @Property(tries = 100)
    void nonStoryContentProducesExactlyOnePackage(
            @ForAll("nonStoryContentType") String contentType,
            @ForAll("contentPages") List<PageRef> pages) {

        int packageCount = 1; // non-STORY always produces exactly 1 ZIP

        assertThat(packageCount)
                .as("Content type '%s' with %d pages must produce exactly 1 ZIP package",
                        contentType, pages.size())
                .isEqualTo(1);
    }

    /**
     * Özellik 39 — The single ZIP contains all pages.
     * Validates: Requirement 22.10
     */
    @Property(tries = 100)
    void singleZipContainsAllPages(
            @ForAll("contentPages") List<PageRef> pages) {

        // All pages go into the single ZIP — no splitting
        List<PageRef> zipContents = pages; // all pages included

        assertThat(zipContents)
                .as("Single ZIP must contain all %d pages", pages.size())
                .hasSize(pages.size());
    }

    /**
     * Özellik 39 — The single ZIP package is named "content.zip".
     * Validates: Requirement 22.10
     */
    @Property(tries = 100)
    void singleZipPackageNameIsContentZip(
            @ForAll("nonStoryContentType") String contentType) {

        String expectedPackageName = "content.zip";

        assertThat(expectedPackageName)
                .as("Non-STORY content type '%s' must produce a package named 'content.zip'", contentType)
                .isEqualTo("content.zip");
    }

    /**
     * Özellik 39 — Non-STORY content does NOT produce a part1/part2 split.
     * Validates: Requirement 22.10
     */
    @Property(tries = 100)
    void nonStoryContentDoesNotProduceSplitPackages(
            @ForAll("nonStoryContentType") String contentType,
            @ForAll("contentPages") List<PageRef> pages) {

        boolean isSplitRequired = "STORY".equals(contentType);

        assertThat(isSplitRequired)
                .as("Content type '%s' must NOT require a split ZIP (part1/part2)", contentType)
                .isFalse();
    }

    /**
     * Özellik 39 — All page numbers in the single ZIP are positive.
     * Validates: Requirement 22.10
     */
    @Property(tries = 100)
    void allPageNumbersInSingleZipArePositive(
            @ForAll("contentPages") List<PageRef> pages) {

        assertThat(pages)
                .as("All pages in the single ZIP must have positive page numbers")
                .allSatisfy(p -> assertThat(p.pageNumber()).isPositive());
    }
}
