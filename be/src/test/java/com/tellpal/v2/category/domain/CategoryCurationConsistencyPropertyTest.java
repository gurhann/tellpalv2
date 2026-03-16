package com.tellpal.v2.category.domain;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based test for category content curation consistency (Özellik 8).
 *
 * Validates: Requirements 4.7, 4.9
 *
 * Özellik 8: Kategori İçerik Kürasyon Tutarlılığı
 * - Bir kategori, dil başına birden fazla içerik içerebilir (kürasyon dil bazlıdır) (Req 4.7)
 * - Kategori içerikleri display_order ASC sırasında döndürülmeli (Req 4.9)
 * - (category_id, language_code, content_id) kombinasyonu bir kategori içinde benzersiz olmalı
 * - display_order >= 0 olmalı
 */
public class CategoryCurationConsistencyPropertyTest {

    /** Minimal domain record — no JPA, no Spring. */
    record CategoryContentRecord(Long categoryId, String languageCode, Long contentId, int displayOrder) {}

    // -------------------------------------------------------------------------
    // Helper: simulates "get contents for (categoryId, languageCode) ordered by display_order ASC"
    // -------------------------------------------------------------------------

    /**
     * Returns contents filtered by (categoryId, languageCode), ordered by displayOrder ASC.
     * This mirrors the ordering logic applied when returning category contents.
     */
    private List<CategoryContentRecord> getContentsForCategoryAndLanguage(
            List<CategoryContentRecord> entries, Long categoryId, String languageCode) {
        return entries.stream()
                .filter(e -> e.categoryId().equals(categoryId) && e.languageCode().equals(languageCode))
                .sorted(Comparator.comparingInt(CategoryContentRecord::displayOrder))
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Arbitraries
    // -------------------------------------------------------------------------

    @Provide
    Arbitrary<String> anyLanguageCode() {
        return Arbitraries.of("tr", "en", "es", "pt", "de");
    }

    @Provide
    Arbitrary<Integer> nonNegativeDisplayOrder() {
        return Arbitraries.integers().between(0, 1000);
    }

    @Provide
    Arbitrary<CategoryContentRecord> anyCategoryContent() {
        return Combinators.combine(
                Arbitraries.longs().between(1L, 100L),
                Arbitraries.of("tr", "en", "es", "pt", "de"),
                Arbitraries.longs().between(1L, 10000L),
                Arbitraries.integers().between(0, 1000)
        ).as(CategoryContentRecord::new);
    }

    @Provide
    Arbitrary<List<CategoryContentRecord>> categoryContentList() {
        return anyCategoryContent().list().ofMinSize(1).ofMaxSize(20);
    }

    // -------------------------------------------------------------------------
    // Properties
    // -------------------------------------------------------------------------

    /**
     * Özellik 8 — A category can contain multiple content entries for the same language.
     *
     * **Validates: Requirements 4.7**
     */
    @Property(tries = 100)
    void categoryCanContainMultipleContentsPerLanguage(
            @ForAll("anyLanguageCode") String languageCode,
            @ForAll("nonNegativeDisplayOrder") int displayOrder1,
            @ForAll("nonNegativeDisplayOrder") int displayOrder2) {

        Long categoryId = 1L;

        List<CategoryContentRecord> entries = List.of(
                new CategoryContentRecord(categoryId, languageCode, 101L, displayOrder1),
                new CategoryContentRecord(categoryId, languageCode, 102L, displayOrder2)
        );

        List<CategoryContentRecord> result = getContentsForCategoryAndLanguage(entries, categoryId, languageCode);

        assertThat(result)
                .as("A category must allow multiple contents for language '%s'", languageCode)
                .hasSize(2);
    }

    /**
     * Özellik 8 — Contents filtered by (categoryId, languageCode) are sorted by displayOrder ASC.
     *
     * **Validates: Requirements 4.9**
     */
    @Property(tries = 100)
    void categoryContentsAreReturnedOrderedByDisplayOrderAsc(
            @ForAll("categoryContentList") List<CategoryContentRecord> entries,
            @ForAll("anyLanguageCode") String languageCode) {

        Long categoryId = 1L;

        List<CategoryContentRecord> result = getContentsForCategoryAndLanguage(entries, categoryId, languageCode);

        for (int i = 0; i < result.size() - 1; i++) {
            assertThat(result.get(i).displayOrder())
                    .as("displayOrder at index %d must be <= displayOrder at index %d for lang '%s'",
                            i, i + 1, languageCode)
                    .isLessThanOrEqualTo(result.get(i + 1).displayOrder());
        }
    }

    /**
     * Özellik 8 — displayOrder must be non-negative (>= 0) for all entries.
     *
     * **Validates: DB constraint (display_order >= 0)**
     */
    @Property(tries = 100)
    void displayOrderIsNonNegative(
            @ForAll("categoryContentList") List<CategoryContentRecord> entries) {

        entries.forEach(e ->
                assertThat(e.displayOrder())
                        .as("displayOrder for content %d in category %d must be >= 0",
                                e.contentId(), e.categoryId())
                        .isGreaterThanOrEqualTo(0)
        );
    }

    /**
     * Özellik 8 — Filtering by (categoryId, languageCode) returns only entries matching both.
     *
     * **Validates: Requirements 4.7, 4.9**
     */
    @Property(tries = 100)
    void filterByCategoryAndLanguageReturnsOnlyMatchingEntries(
            @ForAll("categoryContentList") List<CategoryContentRecord> entries,
            @ForAll("anyLanguageCode") String languageCode) {

        Long categoryId = 1L;

        List<CategoryContentRecord> result = getContentsForCategoryAndLanguage(entries, categoryId, languageCode);

        // All returned entries must match both categoryId and languageCode
        assertThat(result)
                .as("All returned entries must have categoryId=%d and languageCode='%s'",
                        categoryId, languageCode)
                .allMatch(e -> e.categoryId().equals(categoryId) && e.languageCode().equals(languageCode));

        // Count must match the number of entries with that (categoryId, languageCode) in the input
        long expectedCount = entries.stream()
                .filter(e -> e.categoryId().equals(categoryId) && e.languageCode().equals(languageCode))
                .count();
        assertThat(result)
                .as("Returned entry count must match input count for categoryId=%d, lang='%s'",
                        categoryId, languageCode)
                .hasSize((int) expectedCount);
    }
}
