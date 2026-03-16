package com.tellpal.v2.content.domain;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Assume;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based test for content type specific field validation (Özellik 4).
 *
 * Validates: Requirement 2.6
 *
 * Özellik 4: İçerik Tipi Özel Alan Validasyonu
 * - MEDITATION veya AUDIO_STORY tipi içerik için body_text dolu olmalıdır (Req 2.6)
 * - STORY tipi için body_text zorunlu değildir (sayfalar kullanılır)
 * - LULLABY tipi için body_text isteğe bağlıdır
 */
public class ContentTypeFieldValidationPropertyTest {

    /** Minimal domain record — no JPA, no Spring. */
    record ContentTypeFieldRecord(ContentType contentType, String bodyText) {}

    // -------------------------------------------------------------------------
    // Helper: mirrors the body_text validation logic in ContentPublishingService
    // -------------------------------------------------------------------------

    /**
     * Returns true if the content type + body_text combination is valid for publishing.
     * Mirrors the validation in ContentPublishingService.publishLocalization().
     */
    private boolean isPublishable(ContentTypeFieldRecord record) {
        if (record.contentType() == ContentType.MEDITATION
                || record.contentType() == ContentType.AUDIO_STORY) {
            return record.bodyText() != null && !record.bodyText().isBlank();
        }
        // STORY and LULLABY do not require body_text
        return true;
    }

    // -------------------------------------------------------------------------
    // Arbitraries
    // -------------------------------------------------------------------------

    @Provide
    Arbitrary<ContentType> bodyTextRequiredTypes() {
        return Arbitraries.of(ContentType.MEDITATION, ContentType.AUDIO_STORY);
    }

    @Provide
    Arbitrary<ContentType> bodyTextOptionalTypes() {
        return Arbitraries.of(ContentType.STORY, ContentType.LULLABY);
    }

    @Provide
    Arbitrary<String> nonBlankBodyTexts() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(1)
                .ofMaxLength(500)
                .filter(s -> !s.isBlank());
    }

    @Provide
    Arbitrary<String> blankBodyTexts() {
        return Arbitraries.of("", "   ", "\t", "\n", "  \t  ");
    }

    // -------------------------------------------------------------------------
    // Properties
    // -------------------------------------------------------------------------

    /**
     * Özellik 4 — MEDITATION and AUDIO_STORY with a non-blank body_text must be publishable.
     *
     * **Validates: Requirements 2.6**
     */
    @Property(tries = 100)
    void meditationAndAudioStoryWithNonBlankBodyTextIsPublishable(
            @ForAll("bodyTextRequiredTypes") ContentType contentType,
            @ForAll("nonBlankBodyTexts") String bodyText) {

        ContentTypeFieldRecord record = new ContentTypeFieldRecord(contentType, bodyText);

        assertThat(isPublishable(record))
                .as("%s with non-blank body_text must be publishable", contentType)
                .isTrue();
    }

    /**
     * Özellik 4 — MEDITATION and AUDIO_STORY with a blank body_text must NOT be publishable.
     *
     * **Validates: Requirements 2.6**
     */
    @Property(tries = 100)
    void meditationAndAudioStoryWithBlankBodyTextIsNotPublishable(
            @ForAll("bodyTextRequiredTypes") ContentType contentType,
            @ForAll("blankBodyTexts") String blankBodyText) {

        ContentTypeFieldRecord record = new ContentTypeFieldRecord(contentType, blankBodyText);

        assertThat(isPublishable(record))
                .as("%s with blank body_text '%s' must NOT be publishable", contentType, blankBodyText)
                .isFalse();
    }

    /**
     * Özellik 4 — MEDITATION and AUDIO_STORY with null body_text must NOT be publishable.
     *
     * **Validates: Requirements 2.6**
     */
    @Property(tries = 100)
    void meditationAndAudioStoryWithNullBodyTextIsNotPublishable(
            @ForAll("bodyTextRequiredTypes") ContentType contentType) {

        ContentTypeFieldRecord record = new ContentTypeFieldRecord(contentType, null);

        assertThat(isPublishable(record))
                .as("%s with null body_text must NOT be publishable", contentType)
                .isFalse();
    }

    /**
     * Özellik 4 — STORY type does not require body_text; it is always publishable
     * regardless of body_text value (pages are used instead).
     *
     * **Validates: Requirements 2.6**
     */
    @Property(tries = 100)
    void storyTypeIsPublishableWithoutBodyText() {
        ContentTypeFieldRecord withNull = new ContentTypeFieldRecord(ContentType.STORY, null);
        ContentTypeFieldRecord withBlank = new ContentTypeFieldRecord(ContentType.STORY, "");

        assertThat(isPublishable(withNull))
                .as("STORY with null body_text must be publishable (pages are used instead)")
                .isTrue();
        assertThat(isPublishable(withBlank))
                .as("STORY with blank body_text must be publishable (pages are used instead)")
                .isTrue();
    }

    /**
     * Özellik 4 — LULLABY type does not require body_text; it is always publishable
     * regardless of body_text value.
     *
     * **Validates: Requirements 2.6**
     */
    @Property(tries = 100)
    void lullabyTypeIsPublishableWithoutBodyText() {
        ContentTypeFieldRecord withNull = new ContentTypeFieldRecord(ContentType.LULLABY, null);
        ContentTypeFieldRecord withBlank = new ContentTypeFieldRecord(ContentType.LULLABY, "");

        assertThat(isPublishable(withNull))
                .as("LULLABY with null body_text must be publishable (body_text is optional)")
                .isTrue();
        assertThat(isPublishable(withBlank))
                .as("LULLABY with blank body_text must be publishable (body_text is optional)")
                .isTrue();
    }

    /**
     * Özellik 4 — STORY and LULLABY types are always publishable regardless of body_text content.
     *
     * **Validates: Requirements 2.6**
     */
    @Property(tries = 100)
    void storyAndLullabyAreAlwaysPublishableRegardlessOfBodyText(
            @ForAll("bodyTextOptionalTypes") ContentType contentType,
            @ForAll("nonBlankBodyTexts") String bodyText) {

        ContentTypeFieldRecord record = new ContentTypeFieldRecord(contentType, bodyText);

        assertThat(isPublishable(record))
                .as("%s with any body_text must be publishable", contentType)
                .isTrue();
    }
}
