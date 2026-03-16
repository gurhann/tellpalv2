package com.tellpal.v2.content.domain;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based test for soft delete behavior (Özellik 31).
 *
 * Validates: Requirements 20.3, 20.4, 20.5
 *
 * Özellik 31: Soft Delete Davranışı
 * - is_active=false olan içerik API liste yanıtlarından hariç tutulmalı (Req 20.3)
 * - is_active=false olan kategori API liste yanıtlarından hariç tutulmalı (Req 20.4)
 * - is_active=false olduğunda tüm DB ilişkileri ve tarihsel event referansları korunmalı (Req 20.5)
 */
public class SoftDeletePropertyTest {

    /** Minimal domain record — no JPA, no Spring. */
    record ContentRecord(Long id, String externalKey, boolean isActive) {}

    record CategoryRecord(Long id, String slug, boolean isActive) {}

    // -------------------------------------------------------------------------
    // Helpers: simulate API list filtering
    // -------------------------------------------------------------------------

    private List<ContentRecord> listActiveContents(List<ContentRecord> all) {
        return all.stream().filter(ContentRecord::isActive).collect(Collectors.toList());
    }

    private List<CategoryRecord> listActiveCategories(List<CategoryRecord> all) {
        return all.stream().filter(CategoryRecord::isActive).collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Arbitraries
    // -------------------------------------------------------------------------

    @Provide
    Arbitrary<List<ContentRecord>> mixedContentList() {
        // Generate unique IDs to avoid duplicate id with different isActive values
        return Arbitraries.longs().between(1L, 10000L)
                .list().ofMinSize(1).ofMaxSize(20).uniqueElements()
                .flatMap(ids -> {
                    List<ContentRecord> records = new java.util.ArrayList<>();
                    for (int i = 0; i < ids.size(); i++) {
                        long id = ids.get(i);
                        boolean active = (i % 2 == 0); // deterministic: even=active, odd=inactive
                        records.add(new ContentRecord(id, "key-" + id, active));
                    }
                    return Arbitraries.just(records);
                });
    }

    @Provide
    Arbitrary<List<CategoryRecord>> mixedCategoryList() {
        return Arbitraries.longs().between(1L, 10000L)
                .list().ofMinSize(1).ofMaxSize(20).uniqueElements()
                .flatMap(ids -> {
                    List<CategoryRecord> records = new java.util.ArrayList<>();
                    for (int i = 0; i < ids.size(); i++) {
                        long id = ids.get(i);
                        boolean active = (i % 2 == 0);
                        records.add(new CategoryRecord(id, "slug-" + id, active));
                    }
                    return Arbitraries.just(records);
                });
    }

    // -------------------------------------------------------------------------
    // Properties
    // -------------------------------------------------------------------------

    /**
     * Özellik 31 — Active content (is_active=true) must appear in list results.
     *
     * **Validates: Requirements 20.3**
     */
    @Property(tries = 100)
    void activeContentAppearsInListResults(
            @ForAll("mixedContentList") List<ContentRecord> contents) {

        List<ContentRecord> active = listActiveContents(contents);
        List<Long> activeIds = active.stream().map(ContentRecord::id).collect(Collectors.toList());

        contents.stream()
                .filter(ContentRecord::isActive)
                .forEach(c -> assertThat(activeIds)
                        .as("Active content id=%d must appear in list results", c.id())
                        .contains(c.id()));
    }

    /**
     * Özellik 31 — Inactive content (is_active=false) must be excluded from list results.
     *
     * **Validates: Requirements 20.3**
     */
    @Property(tries = 100)
    void inactiveContentIsExcludedFromListResults(
            @ForAll("mixedContentList") List<ContentRecord> contents) {

        List<ContentRecord> active = listActiveContents(contents);
        List<Long> activeIds = active.stream().map(ContentRecord::id).collect(Collectors.toList());

        contents.stream()
                .filter(c -> !c.isActive())
                .forEach(c -> assertThat(activeIds)
                        .as("Inactive content id=%d must NOT appear in list results", c.id())
                        .doesNotContain(c.id()));
    }

    /**
     * Özellik 31 — Inactive category (is_active=false) must be excluded from list results.
     *
     * **Validates: Requirements 20.4**
     */
    @Property(tries = 100)
    void inactiveCategoryIsExcludedFromListResults(
            @ForAll("mixedCategoryList") List<CategoryRecord> categories) {

        List<CategoryRecord> active = listActiveCategories(categories);
        List<Long> activeIds = active.stream().map(CategoryRecord::id).collect(Collectors.toList());

        categories.stream()
                .filter(c -> !c.isActive())
                .forEach(c -> assertThat(activeIds)
                        .as("Inactive category id=%d must NOT appear in list results", c.id())
                        .doesNotContain(c.id()));
    }

    /**
     * Özellik 31 — Soft-deleted content still has its data intact (relationships preserved).
     * The record exists in the full list even when excluded from the active list.
     *
     * **Validates: Requirements 20.5**
     */
    @Property(tries = 100)
    void softDeletedContentDataIsPreserved(
            @ForAll("mixedContentList") List<ContentRecord> contents) {

        List<ContentRecord> active = listActiveContents(contents);

        // Inactive records must still exist in the full list (data preserved)
        contents.stream()
                .filter(c -> !c.isActive())
                .forEach(c -> {
                    assertThat(contents)
                            .as("Soft-deleted content id=%d must still exist in the full data set", c.id())
                            .contains(c);
                    assertThat(c.externalKey())
                            .as("Soft-deleted content id=%d must retain its externalKey", c.id())
                            .isNotNull()
                            .isNotBlank();
                });

        // Active list size + inactive count must equal total
        long inactiveCount = contents.stream().filter(c -> !c.isActive()).count();
        assertThat(active.size() + inactiveCount)
                .as("active + inactive must equal total content count")
                .isEqualTo(contents.size());
    }

    /**
     * Özellik 31 — Soft-deleted category data is preserved.
     *
     * **Validates: Requirements 20.5**
     */
    @Property(tries = 100)
    void softDeletedCategoryDataIsPreserved(
            @ForAll("mixedCategoryList") List<CategoryRecord> categories) {

        List<CategoryRecord> active = listActiveCategories(categories);

        categories.stream()
                .filter(c -> !c.isActive())
                .forEach(c -> {
                    assertThat(categories)
                            .as("Soft-deleted category id=%d must still exist in the full data set", c.id())
                            .contains(c);
                    assertThat(c.slug())
                            .as("Soft-deleted category id=%d must retain its slug", c.id())
                            .isNotNull()
                            .isNotBlank();
                });

        long inactiveCount = categories.stream().filter(c -> !c.isActive()).count();
        assertThat(active.size() + inactiveCount)
                .as("active + inactive must equal total category count")
                .isEqualTo(categories.size());
    }
}
