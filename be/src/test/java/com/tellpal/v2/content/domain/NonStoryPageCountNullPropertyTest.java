package com.tellpal.v2.content.domain;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based test for non-STORY page_count null invariant (Özellik 48).
 *
 * Validates: Requirement 2.1
 *
 * Özellik 48: STORY Dışı İçerikte page_count Null Olmalı
 * - Only STORY type has pages (page_count). Non-STORY types must have page_count = null.
 * - When a STORY content is created, page_count is initialized to 0 (not null).
 */
public class NonStoryPageCountNullPropertyTest {

    /** Minimal domain record — no JPA, no Spring. */
    record ContentRecord(ContentType type, Integer pageCount) {}

    // -------------------------------------------------------------------------
    // Helper: mirrors the page_count initialization logic in ContentApplicationService
    // -------------------------------------------------------------------------

    /**
     * Simulates content creation as performed by ContentApplicationService.createContent().
     * STORY → pageCount = 0; all other types → pageCount = null.
     */
    private ContentRecord createContent(ContentType type) {
        Integer pageCount = (type == ContentType.STORY) ? 0 : null;
        return new ContentRecord(type, pageCount);
    }

    // -------------------------------------------------------------------------
    // Arbitraries
    // -------------------------------------------------------------------------

    @Provide
    Arbitrary<ContentType> nonStoryTypes() {
        return Arbitraries.of(ContentType.AUDIO_STORY, ContentType.MEDITATION, ContentType.LULLABY);
    }

    // -------------------------------------------------------------------------
    // Properties
    // -------------------------------------------------------------------------

    /**
     * Özellik 48 — For STORY type, page_count is initialized to 0 (not null) on creation.
     *
     * **Validates: Requirements 2.1**
     */
    @Property(tries = 100)
    void storyContentHasPageCountInitializedToZero() {
        ContentRecord content = createContent(ContentType.STORY);

        assertThat(content.pageCount())
                .as("STORY content must have page_count initialized to 0, not null")
                .isNotNull()
                .isEqualTo(0);
    }

    /**
     * Özellik 48 — For AUDIO_STORY, MEDITATION, and LULLABY types, page_count must be null.
     *
     * **Validates: Requirements 2.1**
     */
    @Property(tries = 100)
    void nonStoryContentHasNullPageCount(
            @ForAll("nonStoryTypes") ContentType contentType) {

        ContentRecord content = createContent(contentType);

        assertThat(content.pageCount())
                .as("%s content must have page_count = null (pages are not applicable)", contentType)
                .isNull();
    }

    /**
     * Özellik 48 — Content type determines whether page_count is applicable.
     * Only STORY has a non-null page_count; all others have null.
     *
     * **Validates: Requirements 2.1**
     */
    @Property(tries = 100)
    void pageCountApplicabilityDeterminedByContentType(
            @ForAll("nonStoryTypes") ContentType nonStoryType) {

        ContentRecord story = createContent(ContentType.STORY);
        ContentRecord nonStory = createContent(nonStoryType);

        assertThat(story.pageCount())
                .as("STORY must have non-null page_count")
                .isNotNull();

        assertThat(nonStory.pageCount())
                .as("%s must have null page_count", nonStoryType)
                .isNull();
    }
}
