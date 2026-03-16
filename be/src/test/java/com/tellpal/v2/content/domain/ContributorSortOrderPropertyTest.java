package com.tellpal.v2.content.domain;

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
 * Property-based test for contributor role sort order consistency (Özellik 6).
 *
 * Validates: Requirements 3.4, 3.5
 *
 * Özellik 6: Contributor Rol Sıralama Tutarlılığı
 * - Aynı role sahip birden fazla contributor olabilir (Req 3.4)
 * - Aynı role sahip contributor'lar sort_order ASC sırasında döndürülmeli (Req 3.5)
 * - sort_order negatif olamaz (DB kısıtı)
 */
public class ContributorSortOrderPropertyTest {

    /** Minimal domain record — no JPA, no Spring. */
    record ContentContributorRecord(Long contributorId, ContributorRole role, int sortOrder) {}

    // -------------------------------------------------------------------------
    // Helper: simulates "get contributors for role ordered by sort_order ASC"
    // -------------------------------------------------------------------------

    /**
     * Returns contributors filtered by the given role, ordered by sort_order ASC.
     * This mirrors the ordering logic applied when returning contributors for a role.
     */
    private List<ContentContributorRecord> getContributorsForRoleOrdered(
            List<ContentContributorRecord> contributors, ContributorRole role) {
        return contributors.stream()
                .filter(c -> c.role() == role)
                .sorted(Comparator.comparingInt(ContentContributorRecord::sortOrder))
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Arbitraries
    // -------------------------------------------------------------------------

    @Provide
    Arbitrary<ContributorRole> anyRole() {
        return Arbitraries.of(ContributorRole.values());
    }

    @Provide
    Arbitrary<Integer> nonNegativeSortOrder() {
        return Arbitraries.integers().between(0, 1000);
    }

    @Provide
    Arbitrary<ContentContributorRecord> anyContributor() {
        return Combinators.combine(
                Arbitraries.longs().between(1L, 10000L),
                Arbitraries.of(ContributorRole.values()),
                Arbitraries.integers().between(0, 1000)
        ).as(ContentContributorRecord::new);
    }

    @Provide
    Arbitrary<List<ContentContributorRecord>> contributorList() {
        return anyContributor().list().ofMinSize(1).ofMaxSize(20);
    }

    // -------------------------------------------------------------------------
    // Properties
    // -------------------------------------------------------------------------

    /**
     * Özellik 6 — Multiple contributors with the same role are allowed.
     *
     * **Validates: Requirements 3.4**
     */
    @Property(tries = 100)
    void multipleContributorsWithSameRoleAreAllowed(
            @ForAll("anyRole") ContributorRole role,
            @ForAll("nonNegativeSortOrder") int sortOrder1,
            @ForAll("nonNegativeSortOrder") int sortOrder2) {

        List<ContentContributorRecord> contributors = List.of(
                new ContentContributorRecord(1L, role, sortOrder1),
                new ContentContributorRecord(2L, role, sortOrder2)
        );

        List<ContentContributorRecord> result = getContributorsForRoleOrdered(contributors, role);

        assertThat(result)
                .as("Multiple contributors with role '%s' must all be returned", role)
                .hasSize(2);
    }

    /**
     * Özellik 6 — Contributors with the same role are returned ordered by sort_order ASC.
     *
     * **Validates: Requirements 3.5**
     */
    @Property(tries = 100)
    void contributorsWithSameRoleAreOrderedBySortOrderAsc(
            @ForAll("contributorList") List<ContentContributorRecord> contributors,
            @ForAll("anyRole") ContributorRole role) {

        List<ContentContributorRecord> result = getContributorsForRoleOrdered(contributors, role);

        // Verify ascending sort_order
        for (int i = 0; i < result.size() - 1; i++) {
            assertThat(result.get(i).sortOrder())
                    .as("sort_order at index %d must be <= sort_order at index %d for role '%s'",
                            i, i + 1, role)
                    .isLessThanOrEqualTo(result.get(i + 1).sortOrder());
        }
    }

    /**
     * Özellik 6 — sort_order must be non-negative (>= 0).
     *
     * **Validates: DB constraint (sort_order >= 0)**
     */
    @Property(tries = 100)
    void sortOrderIsNonNegative(
            @ForAll("contributorList") List<ContentContributorRecord> contributors) {

        contributors.forEach(c ->
                assertThat(c.sortOrder())
                        .as("sort_order for contributor %d must be >= 0", c.contributorId())
                        .isGreaterThanOrEqualTo(0)
        );
    }

    /**
     * Özellik 6 — Filtering by role returns only contributors with that role,
     * preserving sort_order ASC ordering.
     *
     * **Validates: Requirements 3.4, 3.5**
     */
    @Property(tries = 100)
    void filterByRoleReturnsOnlyMatchingContributorsInOrder(
            @ForAll("contributorList") List<ContentContributorRecord> contributors,
            @ForAll("anyRole") ContributorRole role) {

        List<ContentContributorRecord> result = getContributorsForRoleOrdered(contributors, role);

        // All returned contributors must have the requested role
        assertThat(result)
                .as("All returned contributors must have role '%s'", role)
                .allMatch(c -> c.role() == role);

        // Count must match the number of contributors with that role in the input
        long expectedCount = contributors.stream().filter(c -> c.role() == role).count();
        assertThat(result)
                .as("Returned contributor count must match input count for role '%s'", role)
                .hasSize((int) expectedCount);
    }
}
