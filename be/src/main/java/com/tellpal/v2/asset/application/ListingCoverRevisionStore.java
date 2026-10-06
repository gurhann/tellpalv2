package com.tellpal.v2.asset.application;

import java.util.Optional;

/** Immutable ready manifests are published only after both image variants pass byte verification. */
public interface ListingCoverRevisionStore {
    record Revision(long contentId,String language,long sourceAssetId,String sourceChecksum,String profile,
            String revision,long phoneAssetId,long tabletAssetId,int phoneWidth,int phoneHeight,int tabletWidth,int tabletHeight) {}
    Optional<Revision> find(long contentId,String language,long sourceAssetId,String profile);
    void save(Revision revision);
}
