package com.tellpal.v2.asset.api;

/** Immutable manifest entry for one localized story page. */
public record StoryPageMediaManifestEntry(
        Long pageId,
        int pageNumber,
        Long sourceAssetId,
        AssetRecord illustration,
        Long audioAssetId,
        String sourceChecksumSha256,
        int width,
        int height) {
}
