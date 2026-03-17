package com.tellpal.v2.asset.domain;

public enum ProcessingContentType {

    STORY,
    AUDIO_STORY,
    MEDITATION,
    LULLABY;

    public boolean supportsStoryPackages() {
        return this == STORY;
    }

    public boolean requiresSingleAudioAsset() {
        return this == AUDIO_STORY || this == MEDITATION || this == LULLABY;
    }
}
