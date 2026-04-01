package com.tellpal.v2.category.domain;

import com.tellpal.v2.content.api.ContentApiType;

/**
 * Canonical category types managed by the category aggregate.
 *
 * <p>Category types align one-to-one with the content types they are allowed to curate.
 */
public enum CategoryType {

    STORY,
    AUDIO_STORY,
    MEDITATION,
    LULLABY;

    /**
     * Maps the category type to the external content type contract used by public and admin reads.
     */
    public ContentApiType toContentApiType() {
        return ContentApiType.valueOf(name());
    }
}
