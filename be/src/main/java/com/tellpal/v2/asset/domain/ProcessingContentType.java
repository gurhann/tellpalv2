package com.tellpal.v2.asset.domain;

public enum ProcessingContentType {

    STORY,
    MEDITATION,
    LULLABY;

    public boolean supportsStoryPackages() {
        return this == STORY;
    }

    public boolean requiresSingleAudioAsset() {
        return this == MEDITATION || this == LULLABY;
    }
}
