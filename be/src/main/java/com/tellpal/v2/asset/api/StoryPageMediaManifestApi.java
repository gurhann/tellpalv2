package com.tellpal.v2.asset.api;

import java.util.Optional;

import com.tellpal.v2.shared.domain.LanguageCode;

/** Read boundary for immutable, verified story-page media manifests. */
public interface StoryPageMediaManifestApi {

    /** Returns the newest fully verified manifest for a localized story. */
    Optional<StoryPageMediaManifest> findLatest(Long contentId, LanguageCode languageCode);

    /** String-valued locale lookup for consumers that intentionally avoid shared domain types. */
    Optional<StoryPageMediaManifest> findLatestByLanguageValue(Long contentId, String languageCode);

    /** Returns one retained immutable revision without changing or regenerating its objects. */
    Optional<StoryPageMediaManifest> findRevision(Long contentId, LanguageCode languageCode, String revision);

    /** String-valued immutable revision lookup for consumers that intentionally avoid shared domain types. */
    Optional<StoryPageMediaManifest> findRevisionByLanguageValue(Long contentId, String languageCode, String revision);
}
