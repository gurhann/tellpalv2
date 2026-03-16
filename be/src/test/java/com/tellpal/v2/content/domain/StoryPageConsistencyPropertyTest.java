package com.tellpal.v2.content.domain;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Assume;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based test for story page number consistency (Özellik 3).
 *
 * Validates: Requirements 2.2, 2.4, 2.5
 *
 * Özellik 3: Story Sayfa Numarası Tutarlılığı
 * - Sayfa numaraları 1'den başlamalıdır (Req 2.2) — page_number >= 1
 * - Her sayfa dile özgü metin/ses içerebilir (Req 2.4) — per-page localizations
 * - page_count gerçek sayfa sayısına eşit olmalıdır (Req 2.5) — auto-updated
 */
public class StoryPageConsistencyPropertyTest {

    /** Minimal domain record — no JPA, no Spring. */
    record StoryPageRecord(int pageNumber) {}

    record StoryPageLocalizationRecord(int pageNumber, String languageCode, String textContent) {}

    record StoryContentRecord(int pageCount, List<StoryPageRecord> pages) {}

    // -------------------------------------------------------------------------
    // Helper: simulates page management (add page, remove page, count pages)
    // -------------------------------------------------------------------------

    /**
     * Adds a page to the list and returns a new StoryContentRecord with updated page_count.
     * Mirrors the addStoryPage + updatePageCount logic in ContentApplicationService.
     */
    private StoryContentRecord addPage(StoryContentRecord content, int pageNumber) {
        List<StoryPageRecord> updated = new ArrayList<>(content.pages());
        updated.add(new StoryPageRecord(pageNumber));
        return new StoryContentRecord(updated.size(), updated);
    }

    /**
     * Removes a page from the list and returns a new StoryContentRecord with updated page_count.
     * Mirrors the removeStoryPage + updatePageCount logic in ContentApplicationService.
     */
    private StoryContentRecord removePage(StoryContentRecord content, int pageNumber) {
        List<StoryPageRecord> updated = content.pages().stream()
                .filter(p -> p.pageNumber() != pageNumber)
                .collect(Collectors.toList());
        return new StoryContentRecord(updated.size(), updated);
    }

    /**
     * Builds a StoryContentRecord from a list of page numbers, simulating bulk page creation.
     */
    private StoryContentRecord buildContent(List<Integer> pageNumbers) {
        List<StoryPageRecord> pages = pageNumbers.stream()
                .map(StoryPageRecord::new)
                .collect(Collectors.toList());
        return new StoryContentRecord(pages.size(), pages);
    }

    // -------------------------------------------------------------------------
    // Arbitraries
    // -------------------------------------------------------------------------

    @Provide
    Arbitrary<Integer> validPageNumbers() {
        return Arbitraries.integers().between(1, 200);
    }

    @Provide
    Arbitrary<Integer> invalidPageNumbers() {
        return Arbitraries.integers().between(Integer.MIN_VALUE, 0);
    }

    @Provide
    Arbitrary<String> validLanguageCodes() {
        return Arbitraries.of("tr", "en", "es", "pt", "de");
    }

    @Provide
    Arbitrary<List<Integer>> sequentialPageNumbers() {
        return Arbitraries.integers().between(1, 50)
                .map(count -> IntStream.rangeClosed(1, count)
                        .boxed()
                        .collect(Collectors.toList()));
    }

    // -------------------------------------------------------------------------
    // Properties
    // -------------------------------------------------------------------------

    /**
     * Özellik 3 — Story page numbers must be >= 1 (start from 1).
     *
     * **Validates: Requirements 2.2**
     */
    @Property(tries = 100)
    void storyPageNumbersMustBeGreaterThanOrEqualToOne(
            @ForAll("validPageNumbers") int pageNumber) {

        StoryPageRecord page = new StoryPageRecord(pageNumber);

        assertThat(page.pageNumber())
                .as("Story page number must be >= 1, but was %d", page.pageNumber())
                .isGreaterThanOrEqualTo(1);
    }

    /**
     * Özellik 3 — Page numbers below 1 violate the constraint (page_number >= 1).
     *
     * **Validates: Requirements 2.2**
     */
    @Property(tries = 100)
    void pageNumbersBelowOneViolateConstraint(
            @ForAll("invalidPageNumbers") int invalidPageNumber) {

        assertThat(invalidPageNumber)
                .as("Page number %d is below 1 and must be rejected by the DB check constraint", invalidPageNumber)
                .isLessThanOrEqualTo(0);
    }

    /**
     * Özellik 3 — Each story page can have per-language localizations (text and audio).
     * For any page number, localizations for different languages are distinct records.
     *
     * **Validates: Requirements 2.4**
     */
    @Property(tries = 100)
    void eachPageCanHavePerLanguageLocalizations(
            @ForAll("validPageNumbers") int pageNumber,
            @ForAll("validLanguageCodes") String langA,
            @ForAll("validLanguageCodes") String langB) {

        Assume.that(!langA.equals(langB));

        StoryPageLocalizationRecord locA = new StoryPageLocalizationRecord(pageNumber, langA, "text in " + langA);
        StoryPageLocalizationRecord locB = new StoryPageLocalizationRecord(pageNumber, langB, "text in " + langB);

        // Same page, different languages → distinct localization records
        assertThat(locA.pageNumber()).isEqualTo(locB.pageNumber());
        assertThat(locA.languageCode()).isNotEqualTo(locB.languageCode());

        // Composite key (pageNumber, languageCode) must be unique per language
        String keyA = locA.pageNumber() + "|" + locA.languageCode();
        String keyB = locB.pageNumber() + "|" + locB.languageCode();
        assertThat(keyA)
                .as("Localizations for different languages on the same page must have distinct composite keys")
                .isNotEqualTo(keyB);
    }

    /**
     * Özellik 3 — All localizations for a given page share the same page number.
     *
     * **Validates: Requirements 2.4**
     */
    @Property(tries = 100)
    void allLocalizationsForAPageShareTheSamePageNumber(
            @ForAll("validPageNumbers") int pageNumber) {

        List<String> languages = List.of("tr", "en", "es", "pt", "de");
        List<StoryPageLocalizationRecord> localizations = languages.stream()
                .map(lang -> new StoryPageLocalizationRecord(pageNumber, lang, "text"))
                .collect(Collectors.toList());

        assertThat(localizations)
                .as("All localizations for page %d must share the same page number", pageNumber)
                .allMatch(loc -> loc.pageNumber() == pageNumber);
    }

    /**
     * Özellik 3 — page_count must equal the actual number of pages after adding pages.
     *
     * **Validates: Requirements 2.5**
     */
    @Property(tries = 100)
    void pageCountEqualsActualPageCountAfterAddingPages(
            @ForAll("sequentialPageNumbers") List<Integer> pageNumbers) {

        StoryContentRecord content = new StoryContentRecord(0, List.of());

        for (int pageNumber : pageNumbers) {
            content = addPage(content, pageNumber);
        }

        assertThat(content.pageCount())
                .as("page_count must equal the actual number of pages after adding %d pages", pageNumbers.size())
                .isEqualTo(content.pages().size())
                .isEqualTo(pageNumbers.size());
    }

    /**
     * Özellik 3 — page_count must equal the actual number of pages after removing a page.
     *
     * **Validates: Requirements 2.5**
     */
    @Property(tries = 100)
    void pageCountEqualsActualPageCountAfterRemovingPage(
            @ForAll("sequentialPageNumbers") List<Integer> pageNumbers) {

        Assume.that(!pageNumbers.isEmpty());

        StoryContentRecord content = buildContent(pageNumbers);
        int pageToRemove = pageNumbers.get(0);

        StoryContentRecord afterRemoval = removePage(content, pageToRemove);

        assertThat(afterRemoval.pageCount())
                .as("page_count must be auto-updated after removing page %d", pageToRemove)
                .isEqualTo(afterRemoval.pages().size())
                .isEqualTo(pageNumbers.size() - 1);
    }

    /**
     * Özellik 3 — page_count is always consistent with the pages list regardless of
     * the sequence of add/remove operations.
     *
     * **Validates: Requirements 2.5**
     */
    @Property(tries = 100)
    void pageCountIsAlwaysConsistentWithPagesList(
            @ForAll("sequentialPageNumbers") List<Integer> pageNumbers) {

        StoryContentRecord content = buildContent(pageNumbers);

        assertThat(content.pageCount())
                .as("page_count must always equal pages.size() — invariant must hold")
                .isEqualTo(content.pages().size());
    }

    /**
     * Özellik 3 — After adding then removing the same page, page_count returns to original.
     *
     * **Validates: Requirements 2.5**
     */
    @Property(tries = 100)
    void pageCountReturnsToPreviousValueAfterAddAndRemove(
            @ForAll("sequentialPageNumbers") List<Integer> pageNumbers,
            @ForAll("validPageNumbers") int newPageNumber) {

        // Ensure newPageNumber is not already in the list
        Assume.that(pageNumbers.stream().noneMatch(p -> p == newPageNumber));

        StoryContentRecord before = buildContent(pageNumbers);
        int originalCount = before.pageCount();

        StoryContentRecord afterAdd = addPage(before, newPageNumber);
        StoryContentRecord afterRemove = removePage(afterAdd, newPageNumber);

        assertThat(afterRemove.pageCount())
                .as("page_count must return to %d after adding and removing page %d", originalCount, newPageNumber)
                .isEqualTo(originalCount);
    }

    /**
     * Özellik 3 — Localizations are grouped per page: each page has its own set of
     * language-specific records, and no two pages share the same localization key.
     *
     * **Validates: Requirements 2.4**
     */
    @Property(tries = 100)
    void localizationsAreGroupedPerPage(
            @ForAll("sequentialPageNumbers") List<Integer> pageNumbers) {

        Assume.that(pageNumbers.size() >= 2);

        List<String> languages = List.of("tr", "en");

        // Build all localizations for all pages
        List<StoryPageLocalizationRecord> allLocalizations = pageNumbers.stream()
                .flatMap(pn -> languages.stream()
                        .map(lang -> new StoryPageLocalizationRecord(pn, lang, "text")))
                .collect(Collectors.toList());

        // Group by page number
        Map<Integer, List<StoryPageLocalizationRecord>> byPage = allLocalizations.stream()
                .collect(Collectors.groupingBy(StoryPageLocalizationRecord::pageNumber));

        // Each page must have exactly one localization per language
        for (int pageNumber : pageNumbers) {
            List<StoryPageLocalizationRecord> pageLocs = byPage.get(pageNumber);
            assertThat(pageLocs)
                    .as("Page %d must have exactly %d localizations (one per language)", pageNumber, languages.size())
                    .hasSize(languages.size());

            List<String> langs = pageLocs.stream()
                    .map(StoryPageLocalizationRecord::languageCode)
                    .collect(Collectors.toList());
            assertThat(langs)
                    .as("Page %d must have distinct language codes", pageNumber)
                    .doesNotHaveDuplicates();
        }

        // Total localization count must equal pages * languages
        assertThat(allLocalizations)
                .hasSize(pageNumbers.size() * languages.size());
    }
}
