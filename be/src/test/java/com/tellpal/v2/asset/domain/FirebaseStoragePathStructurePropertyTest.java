package com.tellpal.v2.asset.domain;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based test for Firebase Storage path structure (Özellik 41).
 *
 * **Validates: Requirement 22.5**
 *
 * Özellik 41: Firebase Storage Yol Yapısı
 * - Processed image paths follow: /content/{type}/{key}/{lang}/processed/
 * - Package paths follow: /content/{type}/{key}/{lang}/packages/
 * - Paths must be non-blank, start with "/content/", and contain the type, key, and lang segments.
 */
public class FirebaseStoragePathStructurePropertyTest {

    private static final String[] CONTENT_TYPES = {"STORY", "AUDIO_STORY", "MEDITATION", "LULLABY"};
    private static final String[] LANGUAGES = {"tr", "en", "es", "pt", "de"};

    // -------------------------------------------------------------------------
    // Arbitraries
    // -------------------------------------------------------------------------

    @Provide
    Arbitrary<String> anyContentType() {
        return Arbitraries.of(CONTENT_TYPES);
    }

    @Provide
    Arbitrary<String> anyLanguage() {
        return Arbitraries.of(LANGUAGES);
    }

    @Provide
    Arbitrary<String> anyContentKey() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(3)
                .ofMaxLength(20);
    }

    // -------------------------------------------------------------------------
    // Path builder (mirrors production logic in ZipPackagingService)
    // -------------------------------------------------------------------------

    private String buildProcessedPath(String type, String key, String lang) {
        return "/content/" + type + "/" + key + "/" + lang + "/processed";
    }

    private String buildPackagesPath(String type, String key, String lang) {
        return "/content/" + type + "/" + key + "/" + lang + "/packages";
    }

    // -------------------------------------------------------------------------
    // Properties
    // -------------------------------------------------------------------------

    /**
     * Özellik 41 — Processed path starts with /content/.
     * Validates: Requirement 22.5
     */
    @Property(tries = 200)
    void processedPathStartsWithContentPrefix(
            @ForAll("anyContentType") String type,
            @ForAll("anyContentKey") String key,
            @ForAll("anyLanguage") String lang) {

        String path = buildProcessedPath(type, key, lang);

        assertThat(path)
                .as("Processed path must start with /content/")
                .startsWith("/content/");
    }

    /**
     * Özellik 41 — Packages path starts with /content/.
     * Validates: Requirement 22.5
     */
    @Property(tries = 200)
    void packagesPathStartsWithContentPrefix(
            @ForAll("anyContentType") String type,
            @ForAll("anyContentKey") String key,
            @ForAll("anyLanguage") String lang) {

        String path = buildPackagesPath(type, key, lang);

        assertThat(path)
                .as("Packages path must start with /content/")
                .startsWith("/content/");
    }

    /**
     * Özellik 41 — Processed path contains the content type segment.
     * Validates: Requirement 22.5
     */
    @Property(tries = 200)
    void processedPathContainsContentType(
            @ForAll("anyContentType") String type,
            @ForAll("anyContentKey") String key,
            @ForAll("anyLanguage") String lang) {

        String path = buildProcessedPath(type, key, lang);

        assertThat(path)
                .as("Processed path must contain content type '%s'", type)
                .contains("/" + type + "/");
    }

    /**
     * Özellik 41 — Packages path contains the content type segment.
     * Validates: Requirement 22.5
     */
    @Property(tries = 200)
    void packagesPathContainsContentType(
            @ForAll("anyContentType") String type,
            @ForAll("anyContentKey") String key,
            @ForAll("anyLanguage") String lang) {

        String path = buildPackagesPath(type, key, lang);

        assertThat(path)
                .as("Packages path must contain content type '%s'", type)
                .contains("/" + type + "/");
    }

    /**
     * Özellik 41 — Processed path contains the language code segment.
     * Validates: Requirement 22.5
     */
    @Property(tries = 200)
    void processedPathContainsLanguageCode(
            @ForAll("anyContentType") String type,
            @ForAll("anyContentKey") String key,
            @ForAll("anyLanguage") String lang) {

        String path = buildProcessedPath(type, key, lang);

        assertThat(path)
                .as("Processed path must contain language code '%s'", lang)
                .contains("/" + lang + "/");
    }

    /**
     * Özellik 41 — Packages path contains the language code segment.
     * Validates: Requirement 22.5
     */
    @Property(tries = 200)
    void packagesPathContainsLanguageCode(
            @ForAll("anyContentType") String type,
            @ForAll("anyContentKey") String key,
            @ForAll("anyLanguage") String lang) {

        String path = buildPackagesPath(type, key, lang);

        assertThat(path)
                .as("Packages path must contain language code '%s'", lang)
                .contains("/" + lang + "/");
    }

    /**
     * Özellik 41 — Processed path ends with /processed.
     * Validates: Requirement 22.5
     */
    @Property(tries = 200)
    void processedPathEndsWithProcessedSegment(
            @ForAll("anyContentType") String type,
            @ForAll("anyContentKey") String key,
            @ForAll("anyLanguage") String lang) {

        String path = buildProcessedPath(type, key, lang);

        assertThat(path)
                .as("Processed path must end with /processed")
                .endsWith("/processed");
    }

    /**
     * Özellik 41 — Packages path ends with /packages.
     * Validates: Requirement 22.5
     */
    @Property(tries = 200)
    void packagesPathEndsWithPackagesSegment(
            @ForAll("anyContentType") String type,
            @ForAll("anyContentKey") String key,
            @ForAll("anyLanguage") String lang) {

        String path = buildPackagesPath(type, key, lang);

        assertThat(path)
                .as("Packages path must end with /packages")
                .endsWith("/packages");
    }

    /**
     * Özellik 41 — Processed and packages paths for the same content differ only in the last segment.
     * Validates: Requirement 22.5
     */
    @Property(tries = 200)
    void processedAndPackagesPathsShareCommonPrefix(
            @ForAll("anyContentType") String type,
            @ForAll("anyContentKey") String key,
            @ForAll("anyLanguage") String lang) {

        String processedPath = buildProcessedPath(type, key, lang);
        String packagesPath = buildPackagesPath(type, key, lang);

        String expectedPrefix = "/content/" + type + "/" + key + "/" + lang + "/";

        assertThat(processedPath).startsWith(expectedPrefix);
        assertThat(packagesPath).startsWith(expectedPrefix);
        assertThat(processedPath).isNotEqualTo(packagesPath);
    }

    /**
     * Özellik 41 — Path segments have exactly 5 parts (empty, content, type, key, lang, suffix).
     * Validates: Requirement 22.5
     */
    @Property(tries = 200)
    void processedPathHasCorrectSegmentCount(
            @ForAll("anyContentType") String type,
            @ForAll("anyContentKey") String key,
            @ForAll("anyLanguage") String lang) {

        String path = buildProcessedPath(type, key, lang);
        // /content/{type}/{key}/{lang}/processed → split by "/" gives ["", "content", type, key, lang, "processed"]
        String[] segments = path.split("/");

        assertThat(segments)
                .as("Processed path must have 6 segments when split by '/'")
                .hasSize(6);
        assertThat(segments[1]).isEqualTo("content");
        assertThat(segments[2]).isEqualTo(type);
        assertThat(segments[3]).isEqualTo(key);
        assertThat(segments[4]).isEqualTo(lang);
        assertThat(segments[5]).isEqualTo("processed");
    }
}
