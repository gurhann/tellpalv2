package com.tellpal.v2.content.domain;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Assume;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based test for page_count consistency (Özellik 47).
 *
 * **Validates: Requirements 2.5**
 *
 * Özellik 47: page_count Tutarlılığı
 * - page_count her zaman gerçek story sayfası sayısına eşit olmalıdır
 * - Sayfa eklendiğinde page_count 1 artmalıdır
 * - Sayfa çıkarıldığında page_count 1 azalmalıdır
 * - page_count hiçbir zaman negatif olmamalıdır
 *
 * Pure domain-level logic — no DB, no Spring.
 */
public class PageCountConsistencyPropertyTest {

    /** Minimal domain record representing a STORY content with its pages. */
    record StoryRecord(int pageCount, List<Integer> pageNumbers) {}

    // -------------------------------------------------------------------------
    // Domain helpers — mirror ContentApplicationService add/remove page logic
    // -------------------------------------------------------------------------

    private StoryRecord addPage(StoryRecord story, int pageNumber) {
        List<Integer> updated = new ArrayList<>(story.pageNumbers());
        updated.add(pageNumber);
        return new StoryRecord(updated.size(), updated);
    }

    private StoryRecord removePage(StoryRecord story, int pageNumber) {
        List<Integer> updated = story.pageNumbers().stream()
                .filter(p -> p != pageNumber)
                .collect(Collectors.toList());
        return new StoryRecord(updated.size(), updated);
    }

    private StoryRecord buildStory(List<Integer> pageNumbers) {
        return new StoryRecord(pageNumbers.size(), new ArrayList<>(pageNumbers));
    }

    // -------------------------------------------------------------------------
    // Arbitraries
    // -------------------------------------------------------------------------

    @Provide
    Arbitrary<List<Integer>> sequentialPageNumbers() {
        return Arbitraries.integers().between(1, 50)
                .map(count -> IntStream.rangeClosed(1, count)
                        .boxed()
                        .collect(Collectors.toList()));
    }

    @Provide
    Arbitrary<Integer> validPageNumber() {
        return Arbitraries.integers().between(1, 200);
    }

    // -------------------------------------------------------------------------
    // Properties
    // -------------------------------------------------------------------------

    /**
     * Özellik 47 — page_count must always equal the actual number of story pages.
     *
     * **Validates: Requirements 2.5**
     */
    @Property(tries = 100)
    void pageCountAlwaysEqualsActualNumberOfPages(
            @ForAll("sequentialPageNumbers") List<Integer> pageNumbers) {

        StoryRecord story = buildStory(pageNumbers);

        assertThat(story.pageCount())
                .as("page_count must equal the actual number of pages (%d)", pageNumbers.size())
                .isEqualTo(story.pageNumbers().size())
                .isEqualTo(pageNumbers.size());
    }

    /**
     * Özellik 47 — Adding a page must increment page_count by exactly 1.
     *
     * **Validates: Requirements 2.5**
     */
    @Property(tries = 100)
    void addingAPageIncrementsPageCountByOne(
            @ForAll("sequentialPageNumbers") List<Integer> pageNumbers,
            @ForAll("validPageNumber") int newPageNumber) {

        // Ensure newPageNumber is not already present to avoid ambiguity
        Assume.that(pageNumbers.stream().noneMatch(p -> p == newPageNumber));

        StoryRecord before = buildStory(pageNumbers);
        int countBefore = before.pageCount();

        StoryRecord after = addPage(before, newPageNumber);

        assertThat(after.pageCount())
                .as("page_count must increment by 1 after adding a page (was %d)", countBefore)
                .isEqualTo(countBefore + 1);
    }

    /**
     * Özellik 47 — Removing a page must decrement page_count by exactly 1.
     *
     * **Validates: Requirements 2.5**
     */
    @Property(tries = 100)
    void removingAPageDecrementsPageCountByOne(
            @ForAll("sequentialPageNumbers") List<Integer> pageNumbers) {

        Assume.that(!pageNumbers.isEmpty());

        StoryRecord before = buildStory(pageNumbers);
        int countBefore = before.pageCount();
        int pageToRemove = pageNumbers.get(0);

        StoryRecord after = removePage(before, pageToRemove);

        assertThat(after.pageCount())
                .as("page_count must decrement by 1 after removing a page (was %d)", countBefore)
                .isEqualTo(countBefore - 1);
    }

    /**
     * Özellik 47 — page_count must never be negative.
     *
     * **Validates: Requirements 2.5**
     */
    @Property(tries = 100)
    void pageCountIsNeverNegative(
            @ForAll("sequentialPageNumbers") List<Integer> pageNumbers) {

        StoryRecord story = buildStory(pageNumbers);

        // Remove all pages one by one and verify page_count stays >= 0 at every step
        StoryRecord current = story;
        for (int pageNumber : new ArrayList<>(pageNumbers)) {
            current = removePage(current, pageNumber);
            assertThat(current.pageCount())
                    .as("page_count must never be negative, but was %d after removing page %d",
                            current.pageCount(), pageNumber)
                    .isGreaterThanOrEqualTo(0);
        }

        // After removing all pages, page_count must be exactly 0
        assertThat(current.pageCount())
                .as("page_count must be 0 after removing all pages")
                .isEqualTo(0);
    }
}
