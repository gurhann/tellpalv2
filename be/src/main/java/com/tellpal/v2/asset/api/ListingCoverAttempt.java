package com.tellpal.v2.asset.api;

import java.time.Instant;

/** Captured worker ownership; source/profile and unexpired lease must all match atomically. */
public record ListingCoverAttempt(long processingId,int attemptCount,Instant leaseExpiresAt,
        long sourceAssetId,String profileVersion) {
    public static ListingCoverAttempt captured(AssetProcessingRecord p) {
        return new ListingCoverAttempt(p.processingId(),p.attemptCount(),p.leaseExpiresAt(),p.coverSourceAssetId(),p.listingProfileVersion());
    }
}
