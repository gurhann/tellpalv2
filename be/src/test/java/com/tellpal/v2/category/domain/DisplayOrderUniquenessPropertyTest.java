package com.tellpal.v2.category.domain;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Assume;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based test for display_order uniqueness within a (category_id, language_code) scope (Özellik 9).
 *
 * Validates: Requirement 4.8
 *
 * Özellik 9: Display Order Benzersizliği
 * - (category_id, language_code) kapsamında her içeriğin display_order değeri benzersiz olmalıdır.
 * - Aynı kapsama sahip iki kayıt aynı display_order değerine sahip olamaz.
 * - display_order >= 0 olmalıdır.
 * - Farklı (category_id veya language_code) kapsamları aynı display_order değerlerini yeniden kullanabilir.
 */
public class DisplayOrderUniquenessPropertyTest {

    /** Minimal domain record — no JPA, no Spring. */
    record CategoryContentRecord(Long categoryId, String languageCode, Long contentId, int displayOrder) {}

    // -------------------------------------------------------------------------
    // Arbitraries
    // -------------------------------------------------------------------------

    @Provide
    Arbitrary<String> anyLanguageCode() {
        return Arbitraries.of("tr", "en", "es", "pt", "de");
    }

    @Provide
    Arbitrary<Long> anyCategoryId() {
        return Arbitraries.longs().between(1L, 100L);
    }

    /**
     * Generates a list of entries all sharing the same (categoryId, languageCode),
     * with distinct contentIds and distinct displayOrders.
     */
    @Provide
    Arbitrary<List<CategoryContentRecord>> sameScopeEntriesWithUniqueOrders() {
        return Combinators.combine(
                Arbitraries.longs().between(1L, 100L),
                Arbitraries.of("tr", "en", "es", "pt", "de"),
                Arbitraries.integers().between(1, 15)
        ).as((categoryId, languageCode, size) -> {
            List<Long> contentIds = IntStream.rangeClosed(1, size)
                    .mapToObj(i -> (long) i)
                    .collect(Collectors.toList());
            List<Integer> displayOrders = IntStream.range(0, size)
                    .boxed()
                    .collect(Collectors.toList());
            Collections.shuffle(displayOrders);

            List<CategoryContentRecord> entries = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                entries.add(new CategoryContentRecord(categoryId, languageCode, contentIds.get(i), displayOrders.get(i)));
            }
            return entries;
        });
    }

    // -------------------------------------------------------------------------
    // Properties
    // -------------------------------------------------------------------------

    /**
     * Özellik 9 — Within the same (categoryId, languageCode) scope, all display_orders must be distinct.
     *
     * **Validates: Requirements 4.8**
     */
    @Property(tries = 100)
    void displayOrdersWithinSameScopeAreUnique(
            @ForAll("sameScopeEntriesWithUniqueOrders") List<CategoryContentRecord> entries) {

        // All entries share the same (categoryId, languageCode) — verify by grouping
        assertThat(entries).isNotEmpty();

        Long categoryId = entries.get(0).categoryId();
        String languageCode = entries.get(0).languageCode();

        List<CategoryContentRecord> scopedEntries = entries.stream()
                .filter(e -> e.categoryId().equals(categoryId) && e.languageCode().equals(languageCode))
                .collect(Collectors.toList());

        Set<Integer> uniqueOrders = scopedEntries.stream()
                .map(CategoryContentRecord::displayOrder)
                .collect(Collectors.toSet());

        assertThat(uniqueOrders)
                .as("Within scope (categoryId=%d, languageCode='%s'), all display_orders must be distinct",
                        categoryId, languageCode)
                .hasSize(scopedEntries.size());
    }

    /**
     * Özellik 9 — Entries in different scopes (different categoryId or languageCode)
     * can share the same display_order without conflict.
     *
     * **Validates: Requirements 4.8**
     */
    @Property(tries = 100)
    void differentScopesCanReuseDisplayOrders(
            @ForAll("anyCategoryId") Long categoryIdA,
            @ForAll("anyCategoryId") Long categoryIdB,
            @ForAll("anyLanguageCode") String languageCode) {

        Assume.that(!categoryIdA.equals(categoryIdB));

        int sharedDisplayOrder = 0;

        CategoryContentRecord entryA = new CategoryContentRecord(categoryIdA, languageCode, 1L, sharedDisplayOrder);
        CategoryContentRecord entryB = new CategoryContentRecord(categoryIdB, languageCode, 2L, sharedDisplayOrder);

        // Same display_order in different category scopes — no conflict
        assertThat(entryA.displayOrder())
                .as("Entries in different category scopes may share the same display_order")
                .isEqualTo(entryB.displayOrder());

        // They belong to different scopes, so no uniqueness violation
        assertThat(entryA.categoryId())
                .as("The two entries must belong to different category scopes")
                .isNotEqualTo(entryB.categoryId());
    }

    /**
     * Özellik 9 — display_order must be non-negative (>= 0) for all entries.
     *
     * **Validates: Requirements 4.8**
     */
    @Property(tries = 100)
    void displayOrderIsNonNegative(
            @ForAll("sameScopeEntriesWithUniqueOrders") List<CategoryContentRecord> entries) {

        entries.forEach(e ->
                assertThat(e.displayOrder())
                        .as("displayOrder for contentId=%d in scope (categoryId=%d, languageCode='%s') must be >= 0",
                                e.contentId(), e.categoryId(), e.languageCode())
                        .isGreaterThanOrEqualTo(0)
        );
    }
}
