package com.tellpal.v2.asset.api;

import java.time.Instant;

/** Query-only delivery of verified thumbnails matching the current localized source/profile. */
public interface LibraryCoverQueryApi {
    Covers find(long contentId,String language,Long currentSourceAssetId);
    record Cover(long assetId,String kind,String mimeType,String url,Instant expiresAt) {}
    record Covers(Cover phone,Cover tablet) {public static Covers empty() {return new Covers(null,null);}}
}
