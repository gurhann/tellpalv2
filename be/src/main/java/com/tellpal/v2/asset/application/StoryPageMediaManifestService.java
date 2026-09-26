package com.tellpal.v2.asset.application;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tellpal.v2.asset.api.StoryPageMediaManifest;
import com.tellpal.v2.asset.api.StoryPageMediaManifestApi;
import com.tellpal.v2.shared.domain.LanguageCode;

@Service
public class StoryPageMediaManifestService implements StoryPageMediaManifestApi {

    private final StoryPageMediaManifestStore store;

    public StoryPageMediaManifestService(StoryPageMediaManifestStore store) {
        this.store = store;
    }

    public StoryPageMediaManifest publish(Long contentId, LanguageCode languageCode, String revision,
            String profileVersion, java.util.List<StoryPageMediaManifestStore.Entry> entries, Instant createdAt) {
        return store.save(contentId, languageCode, revision, profileVersion, entries, createdAt);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StoryPageMediaManifest> findLatest(Long contentId, LanguageCode languageCode) {
        if (contentId == null || contentId <= 0 || languageCode == null) {
            throw new IllegalArgumentException("Manifest lookup identity is invalid");
        }
        return store.findLatest(contentId, languageCode);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StoryPageMediaManifest> findLatestByLanguageValue(Long contentId, String languageCode) {
        return findLatest(contentId, LanguageCode.from(languageCode));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StoryPageMediaManifest> findRevision(Long contentId, LanguageCode languageCode, String revision) {
        if (contentId == null || contentId <= 0 || languageCode == null
                || revision == null || !revision.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("Manifest revision lookup identity is invalid");
        }
        return store.findRevision(contentId, languageCode, revision);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StoryPageMediaManifest> findRevisionByLanguageValue(
            Long contentId, String languageCode, String revision) {
        return findRevision(contentId, LanguageCode.from(languageCode), revision);
    }
}
