package com.tellpal.v2.asset.api;

import java.time.Instant;

/** Query-only, source-verified cover access; never delivers pages, narration or packages. */
public interface StoryDetailHeroQueryApi {
    Hero find(long contentId, String language, Long currentSourceAssetId);
    record Hero(long assetId, String checksumSha256, String mimeType, long byteSize,
            String url, Instant expiresAt, int width, int height) {}
}
