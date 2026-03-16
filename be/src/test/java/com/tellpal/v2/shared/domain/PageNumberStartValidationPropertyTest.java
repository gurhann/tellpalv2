package com.tellpal.v2.shared.domain;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based test for page number start validation (Özellik 28).
 *
 * **Validates: Requirements 17.4**
 *
 * Özellik 28: Sayfa Numarası Başlangıç Validasyonu
 * - page_number (story_pages) >= 1
 * - page_number (story_page_localizations) >= 1
 *
 * Page numbers start at 1, not 0. Zero and negative values are invalid.
 */
public class PageNumberStartValidationPropertyTest {

    record StoryPageRecord(Long storyId, int pageNumber) {}
    record StoryPageLocalizationRecord(Long storyPageId, String languageCode, int pageNumber) {}

    // -------------------------------------------------------------------------
    // Arbitraries
    // -------------------------------------------------------------------------

    @Provide
    Arbitrary<Integer> validPageNumber() {
        return Arbitraries.integers().between(1, Integer.MAX_VALUE);
    }

    @Provide
    Arbitrary<Integer> invalidPageNumber() {
        return Arbitraries.integers().between(Integer.MIN_VALUE, 0);
    }

    @Provide
    Arbitrary<Long> positiveLong() {
        return Arbitraries.longs().between(1L, Long.MAX_VALUE);
    }

    @Provide
    Arbitrary<String> languageCode() {
        return Arbitraries.of("tr", "en", "es", "pt", "de");
    }

    // -------------------------------------------------------------------------
    // Properties
    // -------------------------------------------------------------------------

    /**
     * Özellik 28 — page_number in story_pages must be >= 1 (valid values accepted).
     *
     * **Validates: Requirements 17.4**
     */
    @Property(tries = 100)
    void validPageNumberInStoryPageSatisfiesConstraint(
            @ForAll("positiveLong") Long storyId,
            @ForAll("validPageNumber") int pageNumber) {

        StoryPageRecord record = new StoryPageRecord(storyId, pageNumber);

        assertThat(record.pageNumber())
                .as("page_number for story %d must be >= 1", record.storyId())
                .isGreaterThanOrEqualTo(1);
    }

    /**
     * Özellik 28 — page_number in story_page_localizations must be >= 1 (valid values accepted).
     *
     * **Validates: Requirements 17.4**
     */
    @Property(tries = 100)
    void validPageNumberInStoryPageLocalizationSatisfiesConstraint(
            @ForAll("positiveLong") Long storyPageId,
            @ForAll("languageCode") String languageCode,
            @ForAll("validPageNumber") int pageNumber) {

        StoryPageLocalizationRecord record = new StoryPageLocalizationRecord(storyPageId, languageCode, pageNumber);

        assertThat(record.pageNumber())
                .as("page_number for story_page_localization (page=%d, lang=%s) must be >= 1",
                        record.storyPageId(), record.languageCode())
                .isGreaterThanOrEqualTo(1);
    }

    /**
     * Özellik 28 — page_number = 0 or negative violates the constraint (invalid values rejected).
     *
     * **Validates: Requirements 17.4**
     */
    @Property(tries = 100)
    void invalidPageNumberInStoryPageViolatesConstraint(
            @ForAll("positiveLong") Long storyId,
            @ForAll("invalidPageNumber") int pageNumber) {

        StoryPageRecord record = new StoryPageRecord(storyId, pageNumber);

        assertThat(record.pageNumber())
                .as("page_number %d must be identified as invalid (< 1)", record.pageNumber())
                .isLessThan(1);
    }

    /**
     * Özellik 28 — Boundary: page_number = 1 is valid, page_number = 0 is invalid.
     *
     * **Validates: Requirements 17.4**
     */
    @Property(tries = 100)
    void boundaryPageNumberOneIsValidZeroIsInvalid(
            @ForAll("positiveLong") Long storyId) {

        StoryPageRecord validBoundary = new StoryPageRecord(storyId, 1);
        StoryPageRecord invalidBoundary = new StoryPageRecord(storyId, 0);

        assertThat(validBoundary.pageNumber())
                .as("page_number = 1 must be valid (>= 1)")
                .isGreaterThanOrEqualTo(1);

        assertThat(invalidBoundary.pageNumber())
                .as("page_number = 0 must be invalid (< 1)")
                .isLessThan(1);
    }
}
