package com.tellpal.v2.user.web.mobile;

import java.time.Instant;
import java.util.List;

import com.tellpal.v2.asset.api.AssetRecord;
import com.tellpal.v2.asset.api.AssetRegistryApi;
import com.tellpal.v2.asset.api.RefreshMediaAssetDownloadUrlCommand;
import com.tellpal.v2.asset.api.StoryPageMediaManifest;

/** HTTP representation of the protected playback manifest and its short-lived rendition grants. */
public record TestStoryPageMediaManifestResponse(
        int schemaVersion,
        Long contentId,
        String languageCode,
        String revision,
        String profileVersion,
        Instant createdAt,
        List<Entry> pages) {

    public TestStoryPageMediaManifestResponse {
        pages = pages == null ? List.of() : List.copyOf(pages);
    }

    static TestStoryPageMediaManifestResponse from(StoryPageMediaManifest manifest, AssetRegistryApi assetRegistryApi) {
        return new TestStoryPageMediaManifestResponse(
                manifest.schemaVersion(), manifest.contentId(), manifest.languageCodeValue(), manifest.revision(),
                manifest.profileVersion(), manifest.createdAt(), manifest.pages().stream()
                        .map(entry -> new Entry(
                                entry.pageId(), entry.pageNumber(),
                                Asset.from(assetRegistryApi.refreshDownloadUrlCache(
                                        new RefreshMediaAssetDownloadUrlCommand(entry.illustration().assetId()))),
                                entry.audioAssetId(), entry.sourceChecksumSha256(), entry.width(), entry.height()))
                        .toList());
    }

    public record Entry(
            Long pageId,
            int pageNumber,
            Asset illustration,
            Long audioAssetId,
            String sourceChecksumSha256,
            int width,
            int height) {
    }

    public record Asset(
            Long assetId,
            String mimeType,
            Long byteSize,
            String checksumSha256,
            String downloadUrl,
            Instant downloadUrlExpiresAt) {

        static Asset from(AssetRecord asset) {
            return new Asset(asset.assetId(), asset.mimeType(), asset.byteSize(),
                    asset.checksumSha256(), asset.cachedDownloadUrl(), asset.downloadUrlExpiresAt());
        }
    }
}
