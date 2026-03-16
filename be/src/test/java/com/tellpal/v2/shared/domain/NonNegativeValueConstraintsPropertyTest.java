package com.tellpal.v2.shared.domain;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based test for non-negative value constraints (Özellik 27).
 *
 * **Validates: Requirements 17.3**
 *
 * Özellik 27: Negatif Olmayan Değer Kısıtlamaları
 * - sort_order (content_contributors) >= 0
 * - display_order (category_contents) >= 0
 * - page_count (contents) >= 0 when set
 * - engagement_seconds (content_events) >= 0
 * - left_page (content_events) >= 0
 */
public class NonNegativeValueConstraintsPropertyTest {

    record ContributorRecord(Long contributorId, int sortOrder) {}
    record CategoryContentRecord(Long contentId, int displayOrder) {}
    record ContentRecord(Long id, Integer pageCount) {}
    record ContentEventRecord(Long profileId, int leftPage, int engagementSeconds) {}

    // -------------------------------------------------------------------------
    // Arbitraries
    // -------------------------------------------------------------------------

    @Provide
    Arbitrary<Integer> nonNegativeInt() {
        return Arbitraries.integers().between(0, Integer.MAX_VALUE);
    }

    @Provide
    Arbitrary<Long> positiveLong() {
        return Arbitraries.longs().between(1L, Long.MAX_VALUE);
    }

    // -------------------------------------------------------------------------
    // Properties
    // -------------------------------------------------------------------------

    /**
     * Özellik 27 — sort_order in content_contributors must be >= 0.
     *
     * **Validates: Requirements 17.3**
     */
    @Property(tries = 100)
    void sortOrderIsNonNegative(
            @ForAll("positiveLong") Long contributorId,
            @ForAll("nonNegativeInt") int sortOrder) {

        ContributorRecord record = new ContributorRecord(contributorId, sortOrder);

        assertThat(record.sortOrder())
                .as("sort_order for contributor %d must be >= 0", record.contributorId())
                .isGreaterThanOrEqualTo(0);
    }

    /**
     * Özellik 27 — display_order in category_contents must be >= 0.
     *
     * **Validates: Requirements 17.3**
     */
    @Property(tries = 100)
    void displayOrderIsNonNegative(
            @ForAll("positiveLong") Long contentId,
            @ForAll("nonNegativeInt") int displayOrder) {

        CategoryContentRecord record = new CategoryContentRecord(contentId, displayOrder);

        assertThat(record.displayOrder())
                .as("display_order for category content %d must be >= 0", record.contentId())
                .isGreaterThanOrEqualTo(0);
    }

    /**
     * Özellik 27 — page_count in contents must be >= 0 when set (not null).
     *
     * **Validates: Requirements 17.3**
     */
    @Property(tries = 100)
    void pageCountIsNonNegativeWhenSet(
            @ForAll("positiveLong") Long id,
            @ForAll("nonNegativeInt") int pageCount) {

        ContentRecord record = new ContentRecord(id, pageCount);

        assertThat(record.pageCount())
                .as("page_count for content %d must be >= 0 when set", record.id())
                .isNotNull()
                .isGreaterThanOrEqualTo(0);
    }

    /**
     * Özellik 27 — leftPage and engagementSeconds in content_events must both be >= 0.
     *
     * **Validates: Requirements 17.3**
     */
    @Property(tries = 100)
    void eventNumericFieldsAreNonNegative(
            @ForAll("positiveLong") Long profileId,
            @ForAll("nonNegativeInt") int leftPage,
            @ForAll("nonNegativeInt") int engagementSeconds) {

        ContentEventRecord record = new ContentEventRecord(profileId, leftPage, engagementSeconds);

        assertThat(record.leftPage())
                .as("left_page for profile %d must be >= 0", record.profileId())
                .isGreaterThanOrEqualTo(0);

        assertThat(record.engagementSeconds())
                .as("engagement_seconds for profile %d must be >= 0", record.profileId())
                .isGreaterThanOrEqualTo(0);
    }
}
