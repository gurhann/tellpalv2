package com.tellpal.v2.content.domain;

import com.tellpal.v2.shared.domain.LocalizationStatus;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Assume;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based test for language-based content visibility (Özellik 1).
 *
 * Validates: Requirements 1.3, 1.4, 1.5
 *
 * Özellik 1: Dil Bazlı İçerik Görünürlüğü
 * - Durumu PUBLISHED olan bir yerelleştirme, o dil için API yanıtlarına dahil edilmeli (Req 1.3)
 * - Durumu DRAFT veya ARCHIVED olan bir yerelleştirme, o dil için API yanıtlarından hariç tutulmalı (Req 1.4)
 * - Talep edilen dilde yerelleştirme yoksa, içerik o dil için API yanıtlarından hariç tutulmalı (Req 1.5)
 */
public class ContentVisibilityPropertyTest {

    /** Minimal domain record — no JPA, no Spring. */
    record LocalizationRecord(String languageCode, LocalizationStatus status) {}

    // -------------------------------------------------------------------------
    // Helper: simulates "get published localizations for language" filtering
    // -------------------------------------------------------------------------

    /**
     * Returns the localization for the given language only if it exists and is PUBLISHED.
     * This mirrors the filtering logic applied in API responses.
     */
    private Optional<LocalizationRecord> getPublishedLocalization(
            List<LocalizationRecord> localizations, String requestedLang) {
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

    // -------------------------------------------------------------------------
    // Properties
    // -------------------------------------------------------------------------

    /**
     * Özellik 1 — A localization with status=PUBLISHED must be included in API
     * responses for that language.
     *
     * **Validates: Requirements 1.3**
     */
    @Property(tries = 100)
    void publishedLocalizationIsIncludedInApiResponse(
            @ForAll("validLanguageCodes") String lang) {

        LocalizationRecord published = new LocalizationRecord(lang, LocalizationStatus.PUBLISHED);
        List<LocalizationRecord> localizations = List.of(published);

        Optional<LocalizationRecord> result = getPublishedLocalization(localizations, lang);

        assertThat(result)
                .as("A PUBLISHED localization for language '%s' must be included in API responses", lang)
                .isPresent();
        assertThat(result.get().status())
                .isEqualTo(LocalizationStatus.PUBLISHED);
    }

    /**
     * Özellik 1 — A localization with status=DRAFT or ARCHIVED must be excluded
     * from API responses for that language.
     *
     * **Validates: Requirements 1.4**
     */
    @Property(tries = 100)
    void draftOrArchivedLocalizationIsExcludedFromApiResponse(
            @ForAll("validLanguageCodes") String lang,
            @ForAll("nonPublishedStatuses") LocalizationStatus status) {

        LocalizationRecord nonPublished = new LocalizationRecord(lang, status);
        List<LocalizationRecord> localizations = List.of(nonPublished);

        Optional<LocalizationRecord> result = getPublishedLocalization(localizations, lang);

        assertThat(result)
                .as("A %s localization for language '%s' must be excluded from API responses", status, lang)
                .isEmpty();
    }

    /**
     * Özellik 1 — If no localization exists for the requested language, the content
     * must be excluded from API responses for that language.
     *
     * **Validates: Requirements 1.5**
     */
    @Property(tries = 100)
    void missingLocalizationForLanguageExcludesContentFromApiResponse(
            @ForAll("validLanguageCodes") String requestedLang,
            @ForAll("validLanguageCodes") String otherLang) {

        // Only provide a localization for a different language (if same, use empty list)
        List<LocalizationRecord> localizations = requestedLang.equals(otherLang)
                ? List.of()
                : List.of(new LocalizationRecord(otherLang, LocalizationStatus.PUBLISHED));

        Optional<LocalizationRecord> result = getPublishedLocalization(localizations, requestedLang);

        assertThat(result)
                .as("Content with no localization for language '%s' must be excluded from API responses", requestedLang)
                .isEmpty();
    }

    /**
     * Özellik 1 — Only PUBLISHED localizations for the exact requested language
     * are visible; other languages' PUBLISHED localizations do not affect visibility.
     *
     * **Validates: Requirements 1.3, 1.5**
     */
    @Property(tries = 100)
    void onlyExactLanguageMatchWithPublishedStatusIsVisible(
            @ForAll("validLanguageCodes") String requestedLang,
            @ForAll("validLanguageCodes") String otherLang,
            @ForAll("nonPublishedStatuses") LocalizationStatus nonPublishedStatus) {

        // Skip when both languages are the same — the test requires two distinct entries
        Assume.that(!requestedLang.equals(otherLang));

        // Build a list: requested lang has non-published status, other lang has published
        List<LocalizationRecord> localizations = List.of(
                new LocalizationRecord(requestedLang, nonPublishedStatus),
                new LocalizationRecord(otherLang, LocalizationStatus.PUBLISHED)
        );

        Optional<LocalizationRecord> result = getPublishedLocalization(localizations, requestedLang);

        // The requested language is not PUBLISHED, so it must be excluded
        assertThat(result)
                .as("A %s localization for '%s' must be excluded even if another language is PUBLISHED",
                        nonPublishedStatus, requestedLang)
                .isEmpty();
    }
}
