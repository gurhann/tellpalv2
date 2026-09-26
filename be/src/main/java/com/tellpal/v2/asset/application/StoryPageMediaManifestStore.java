package com.tellpal.v2.asset.application;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.tellpal.v2.asset.api.AssetRecord;
import com.tellpal.v2.asset.api.StoryPageMediaManifest;
import com.tellpal.v2.shared.domain.LanguageCode;

public interface StoryPageMediaManifestStore {

    StoryPageMediaManifest save(Long contentId, LanguageCode languageCode, String revision,
            String profileVersion, List<Entry> entries, Instant createdAt);

    Optional<StoryPageMediaManifest> findLatest(Long contentId, LanguageCode languageCode);

    Optional<StoryPageMediaManifest> findRevision(Long contentId, LanguageCode languageCode, String revision);

    record Entry(Long pageId, int pageNumber, Long sourceAssetId, AssetRecord rendition,
            Long audioAssetId, String sourceChecksumSha256, int width, int height) {
    }
}
