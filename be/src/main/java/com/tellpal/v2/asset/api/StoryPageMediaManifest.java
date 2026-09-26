package com.tellpal.v2.asset.api;

import java.time.Instant;
import java.util.List;

import com.tellpal.v2.shared.domain.LanguageCode;

/** Immutable, revision-addressed page media snapshot for the protected playback experiment. */
public record StoryPageMediaManifest(
        int schemaVersion,
        Long contentId,
        LanguageCode languageCode,
        String revision,
        String profileVersion,
        Instant createdAt,
        List<StoryPageMediaManifestEntry> pages) {

    public StoryPageMediaManifest {
        if (schemaVersion <= 0 || contentId == null || contentId <= 0 || languageCode == null
                || revision == null || !revision.matches("[0-9a-f]{64}")
                || profileVersion == null || profileVersion.isBlank() || createdAt == null) {
            throw new IllegalArgumentException("Story page media manifest identity is invalid");
        }
        pages = pages == null ? List.of() : List.copyOf(pages);
    }

    public String languageCodeValue() {
        return languageCode.value();
    }
}
