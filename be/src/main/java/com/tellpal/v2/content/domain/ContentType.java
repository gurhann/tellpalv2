package com.tellpal.v2.content.domain;

/**
 * Canonical content types managed by the content aggregate.
 */
public enum ContentType {

    STORY,
    AUDIO_STORY,
    MEDITATION,
    LULLABY;

    public boolean supportsStoryPages() {
        return this == STORY;
    }
}
