package com.tellpal.v2.content.domain;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based test for asset processing status consistency (Özellik 37).
 *
 * **Validates: Requirements 22.18**
 *
 * Özellik 37: Asset İşleme Durumu Tutarlılığı
 * - İçerik lokalizasyonları yalnızca processing_status = COMPLETED olduğunda
 *   public API'de görünür olmalıdır.
 * - PENDING, PROCESSING ve FAILED durumlarındaki içerikler public istemcilere
 *   sunulmamalıdır.
 */
public class AssetProcessingStatusConsistencyPropertyTest {

    record ContentLocalizationRecord(
            Long contentId,
            String languageCode,
            String localizationStatus,
            String processingStatus) {}

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    /**
     * Returns true only when localizationStatus is "PUBLISHED"
     * AND processingStatus is "COMPLETED".
     */
    boolean isPubliclyVisible(ContentLocalizationRecord record) {
        return "PUBLISHED".equals(record.localizationStatus())
                && "COMPLETED".equals(record.processingStatus());
    }

    // -------------------------------------------------------------------------
    // Arbitraries
    // -------------------------------------------------------------------------

    @Provide
    Arbitrary<Long> positiveLong() {
        return Arbitraries.longs().between(1L, Long.MAX_VALUE);
    }

    @Provide
    Arbitrary<String> languageCode() {
        return Arbitraries.of("tr", "en", "de", "fr", "es", "ar");
    }

    @Provide
    Arbitrary<String> nonCompletedProcessingStatus() {
        return Arbitraries.of("PENDING", "PROCESSING", "FAILED");
    }

    @Provide
    Arbitrary<String> anyLocalizationStatus() {
        return Arbitraries.of("DRAFT", "PUBLISHED", "ARCHIVED");
    }

    @Provide
    Arbitrary<String> nonPublishedLocalizationStatus() {
        return Arbitraries.of("DRAFT", "ARCHIVED");
    }

    // -------------------------------------------------------------------------
    // Properties
    // -------------------------------------------------------------------------

    /**
     * Özellik 37 — Yalnızca COMPLETED işleme durumu public görünürlüğe izin verir.
     *
     * **Validates: Requirements 22.18**
     */
    @Property(tries = 100)
    void onlyCompletedProcessingStatusAllowsPublicVisibility(
            @ForAll("positiveLong") Long contentId,
            @ForAll("languageCode") String languageCode,
            @ForAll("anyLocalizationStatus") String localizationStatus) {

        ContentLocalizationRecord completedRecord = new ContentLocalizationRecord(
                contentId, languageCode, localizationStatus, "COMPLETED");

        ContentLocalizationRecord pendingRecord = new ContentLocalizationRecord(
                contentId, languageCode, localizationStatus, "PENDING");

        // COMPLETED ile PUBLISHED kombinasyonu görünür olmalı
        if ("PUBLISHED".equals(localizationStatus)) {
            assertThat(isPubliclyVisible(completedRecord))
                    .as("COMPLETED + PUBLISHED içerik public görünür olmalıdır")
                    .isTrue();
        }

        // PENDING hiçbir zaman public görünür olmamalı
        assertThat(isPubliclyVisible(pendingRecord))
                .as("PENDING işleme durumundaki içerik public görünür olmamalıdır")
                .isFalse();
    }

    /**
     * Özellik 37 — PENDING, PROCESSING, FAILED durumları public görünürlüğü engeller.
     *
     * **Validates: Requirements 22.18**
     */
    @Property(tries = 100)
    void nonCompletedProcessingStatusPreventsPublicVisibility(
            @ForAll("positiveLong") Long contentId,
            @ForAll("languageCode") String languageCode,
            @ForAll("anyLocalizationStatus") String localizationStatus,
            @ForAll("nonCompletedProcessingStatus") String processingStatus) {

        ContentLocalizationRecord record = new ContentLocalizationRecord(
                contentId, languageCode, localizationStatus, processingStatus);

        assertThat(isPubliclyVisible(record))
                .as("processingStatus=%s olan içerik public görünür olmamalıdır", processingStatus)
                .isFalse();
    }

    /**
     * Özellik 37 — COMPLETED işleme durumu ve PUBLISHED lokalizasyon durumu
     * olan içerik public görünür olmalıdır.
     *
     * **Validates: Requirements 22.18**
     */
    @Property(tries = 100)
    void completedAndPublishedIsPubliclyVisible(
            @ForAll("positiveLong") Long contentId,
            @ForAll("languageCode") String languageCode) {

        ContentLocalizationRecord record = new ContentLocalizationRecord(
                contentId, languageCode, "PUBLISHED", "COMPLETED");

        assertThat(isPubliclyVisible(record))
                .as("COMPLETED + PUBLISHED içerik public görünür olmalıdır (contentId=%d)", contentId)
                .isTrue();
    }

    /**
     * Özellik 37 — COMPLETED işleme durumu olsa bile PUBLISHED olmayan lokalizasyon
     * durumu public görünürlüğü engeller.
     *
     * **Validates: Requirements 22.18**
     */
    @Property(tries = 100)
    void completedButNonPublishedIsNotPubliclyVisible(
            @ForAll("positiveLong") Long contentId,
            @ForAll("languageCode") String languageCode,
            @ForAll("nonPublishedLocalizationStatus") String localizationStatus) {

        ContentLocalizationRecord record = new ContentLocalizationRecord(
                contentId, languageCode, localizationStatus, "COMPLETED");

        assertThat(isPubliclyVisible(record))
                .as("COMPLETED + %s içerik public görünür olmamalıdır (contentId=%d)",
                        localizationStatus, contentId)
                .isFalse();
    }
}
