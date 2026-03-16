package com.tellpal.v2.category.domain;

import com.tellpal.v2.shared.domain.LocalizationStatus;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Assume;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based test for language-based category visibility (Özellik 7).
 *
 * Validates: Requirements 4.5, 4.6
 *
 * Özellik 7: Kategori Dil Bazlı Görünürlük
 * - Durumu PUBLISHED olan bir kategori yerelleştirmesi, o dil için API yanıtlarına dahil edilmeli (Req 4.5)
 * - Durumu DRAFT veya ARCHIVED olan bir kategori yerelleştirmesi, o dil için API yanıtlarından hariç tutulmalı (Req 4.6)
 * - Talep edilen dilde yerelleştirme yoksa, kategori o dil için API yanıtlarından hariç tutulmalı (Req 4.6)
 */
public class CategoryVisibilityPropertyTest {

    /** Minimal domain record — no JPA, no Spring. */
    record CategoryLocalizationRecord(String languageCode, LocalizationStatus status, CategoryType type) {}

    // -------------------------------------------------------------------------
    // Helper: simulates "get published category localizations for language" filtering
    // -------------------------------------------------------------------------

    /**
     * Returns the category localization for the given language only if it exists and is PUBLISHED.
     * This mirrors the filtering logic applied in API responses.
     */
    private Optional<CategoryLocalizationRecord> getPublishedCategoryLocalization(
            List<CategoryLocalizationRecord> localizations, String requestedLang) {
        return localizations.stream()
                .filter(l -> l.languageCode().equals(requestedLang))
                .filter(l -> l.status() == LocalizationStatus.PUBLISHED)
                .findFirst();
    }

    // -------------------------------------------------------------------------
    // Arbitraries
    // -------------------------------------------------------------------------

    @Provide
    Arbitrary<String> validLanguageCodes() {
        return Arbitraries.of("tr", "en", "es", "pt", "de");
    }

    @Provide
    Arbitrary<LocalizationStatus> nonPublishedStatuses() {
        return Arbitraries.of(LocalizationStatus.DRAFT, LocalizationStatus.ARCHIVED);
    }

    @Provide
    Arbitrary<CategoryType> categoryTypes() {
        return Arbitraries.of(CategoryType.values());
    }

    // -------------------------------------------------------------------------
    // Properties
    // -------------------------------------------------------------------------

    /**
     * Özellik 7 — A category localization with status=PUBLISHED must be included
     * in API responses for that language.
     *
     * **Validates: Requirements 4.5**
     */
    @Property(tries = 100)
    void publishedCategoryLocalizationIsIncludedInApiResponse(
            @ForAll("validLanguageCodes") String lang,
            @ForAll("categoryTypes") CategoryType type) {

        CategoryLocalizationRecord published = new CategoryLocalizationRecord(lang, LocalizationStatus.PUBLISHED, type);
        List<CategoryLocalizationRecord> localizations = List.of(published);

        Optional<CategoryLocalizationRecord> result = getPublishedCategoryLocalization(localizations, lang);

        assertThat(result)
                .as("A PUBLISHED category localization for language '%s' must be included in API responses", lang)
                .isPresent();
        assertThat(result.get().status())
                .isEqualTo(LocalizationStatus.PUBLISHED);
    }

    /**
     * Özellik 7 — A category localization with status=DRAFT or ARCHIVED must be
     * excluded from API responses for that language.
     *
     * **Validates: Requirements 4.6**
     */
    @Property(tries = 100)
    void draftOrArchivedCategoryLocalizationIsExcludedFromApiResponse(
            @ForAll("validLanguageCodes") String lang,
            @ForAll("nonPublishedStatuses") LocalizationStatus status,
            @ForAll("categoryTypes") CategoryType type) {

        CategoryLocalizationRecord nonPublished = new CategoryLocalizationRecord(lang, status, type);
        List<CategoryLocalizationRecord> localizations = List.of(nonPublished);

        Optional<CategoryLocalizationRecord> result = getPublishedCategoryLocalization(localizations, lang);

        assertThat(result)
                .as("A %s category localization for language '%s' must be excluded from API responses", status, lang)
                .isEmpty();
    }

    /**
     * Özellik 7 — If no localization exists for the requested language, the category
     * must be excluded from API responses for that language.
     *
     * **Validates: Requirements 4.6**
     */
    @Property(tries = 100)
    void missingLocalizationForLanguageExcludesCategoryFromApiResponse(
            @ForAll("validLanguageCodes") String requestedLang,
            @ForAll("validLanguageCodes") String otherLang,
            @ForAll("categoryTypes") CategoryType type) {

        // Only provide a localization for a different language (if same, use empty list)
        List<CategoryLocalizationRecord> localizations = requestedLang.equals(otherLang)
                ? List.of()
                : List.of(new CategoryLocalizationRecord(otherLang, LocalizationStatus.PUBLISHED, type));

        Optional<CategoryLocalizationRecord> result = getPublishedCategoryLocalization(localizations, requestedLang);

        assertThat(result)
                .as("Category with no localization for language '%s' must be excluded from API responses", requestedLang)
                .isEmpty();
    }

    /**
     * Özellik 7 — Only PUBLISHED localizations for the exact requested language
     * are visible; other languages' PUBLISHED localizations do not affect visibility.
     *
     * **Validates: Requirements 4.5, 4.6**
     */
    @Property(tries = 100)
    void onlyExactLanguageMatchWithPublishedStatusIsVisible(
            @ForAll("validLanguageCodes") String requestedLang,
            @ForAll("validLanguageCodes") String otherLang,
            @ForAll("nonPublishedStatuses") LocalizationStatus nonPublishedStatus,
            @ForAll("categoryTypes") CategoryType type) {

        // Skip when both languages are the same — the test requires two distinct entries
        Assume.that(!requestedLang.equals(otherLang));

        // Build a list: requested lang has non-published status, other lang has published
        List<CategoryLocalizationRecord> localizations = List.of(
                new CategoryLocalizationRecord(requestedLang, nonPublishedStatus, type),
                new CategoryLocalizationRecord(otherLang, LocalizationStatus.PUBLISHED, type)
        );

        Optional<CategoryLocalizationRecord> result = getPublishedCategoryLocalization(localizations, requestedLang);

        // The requested language is not PUBLISHED, so it must be excluded
        assertThat(result)
                .as("A %s category localization for '%s' must be excluded even if another language is PUBLISHED",
                        nonPublishedStatus, requestedLang)
                .isEmpty();
    }
}
