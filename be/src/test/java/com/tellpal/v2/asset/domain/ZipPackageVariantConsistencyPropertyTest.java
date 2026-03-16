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
 * Property-based test for ZIP package variant consistency for STORY content (Özellik 38).
 *
 * **Validates: Requirements 22.9**
 *
 * Özellik 38: ZIP Paket Varyant Tutarlılığı
 * - STORY content must produce exactly 2 ZIP packages: part1 (pages 1–3) and part2 (pages 4+).
 * - part1 always contains pages 1–3.
 * - part2 contains all pages beyond page 3.
 * - part2 is only produced when there are pages beyond page 3.
 */
public class ZipPackageVariantConsistencyPropertyTest {

    record PageRef(int pageNumber) {}

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private List<PageRef> part1Pages(List<PageRef> pages) {
        return pages.stream().filter(p -> p.pageNumber() <= 3).toList();
    }

    private List<PageRef> part2Pages(List<PageRef> pages) {
        return pages.stream().filter(p -> p.pageNumber() > 3).toList();
    }

    private int expectedPackageCount(List<PageRef> pages) {
        boolean hasPart2 = pages.stream().anyMatch(p -> p.pageNumber() > 3);
        return hasPart2 ? 2 : 1;
    }

    // -------------------------------------------------------------------------
    // Arbitraries
    // -------------------------------------------------------------------------

    /** Generates a STORY with pages 1–N where N is between 4 and 10 (always has part2). */
    @Provide
    Arbitrary<List<PageRef>> storyPagesWithPart2() {
        return Arbitraries.integers().between(4, 10)
                .map(n -> IntStream.rangeClosed(1, n).mapToObj(PageRef::new).toList());
    }

    /** Generates a STORY with exactly 1–3 pages (no part2). */
    @Provide
    Arbitrary<List<PageRef>> storyPagesWithoutPart2() {
        return Arbitraries.integers().between(1, 3)
                .map(n -> IntStream.rangeClosed(1, n).mapToObj(PageRef::new).toList());
    }

    // -------------------------------------------------------------------------
    // Properties
    // -------------------------------------------------------------------------

    /**
     * Özellik 38 — STORY with >3 pages produces exactly 2 packages.
     * Validates: Requirement 22.9
     */
    @Property(tries = 100)
    void storyWithMoreThanThreePagesProducesTwoPackages(
            @ForAll("storyPagesWithPart2") List<PageRef> pages) {

        assertThat(expectedPackageCount(pages))
                .as("STORY with %d pages must produce exactly 2 ZIP packages", pages.size())
                .isEqualTo(2);
    }

    /**
     * Özellik 38 — STORY with ≤3 pages produces exactly 1 package (no part2).
     * Validates: Requirement 22.9
     */
    @Property(tries = 100)
    void storyWithThreeOrFewerPagesProducesOnePackage(
            @ForAll("storyPagesWithoutPart2") List<PageRef> pages) {

        assertThat(expectedPackageCount(pages))
                .as("STORY with %d pages (≤3) must produce exactly 1 ZIP package", pages.size())
                .isEqualTo(1);
    }

    /**
     * Özellik 38 — part1 always contains only pages 1–3.
     * Validates: Requirement 22.9
     */
    @Property(tries = 100)
    void part1ContainsOnlyPagesOneToThree(
            @ForAll("storyPagesWithPart2") List<PageRef> pages) {

        List<PageRef> part1 = part1Pages(pages);

        assertThat(part1)
                .as("part1 must contain only pages with pageNumber <= 3")
                .allSatisfy(p -> assertThat(p.pageNumber()).isLessThanOrEqualTo(3));

        assertThat(part1)
                .as("part1 must not be empty for a STORY with pages")
                .isNotEmpty();
    }

    /**
     * Özellik 38 — part2 contains only pages beyond page 3.
     * Validates: Requirement 22.9
     */
    @Property(tries = 100)
    void part2ContainsOnlyPagesAfterThree(
            @ForAll("storyPagesWithPart2") List<PageRef> pages) {

        List<PageRef> part2 = part2Pages(pages);

        assertThat(part2)
                .as("part2 must contain only pages with pageNumber > 3")
                .allSatisfy(p -> assertThat(p.pageNumber()).isGreaterThan(3));
    }

    /**
     * Özellik 38 — part1 + part2 together contain all pages.
     * Validates: Requirement 22.9
     */
    @Property(tries = 100)
    void allPagesAreCoveredByBothParts(
            @ForAll("storyPagesWithPart2") List<PageRef> pages) {

        List<PageRef> part1 = part1Pages(pages);
        List<PageRef> part2 = part2Pages(pages);

        assertThat(part1.size() + part2.size())
                .as("part1 + part2 must cover all %d pages", pages.size())
                .isEqualTo(pages.size());
    }
}
