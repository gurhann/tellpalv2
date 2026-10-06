package com.tellpal.v2.asset.api;

/** Explicit, source-bound preparation; never called by catalog GET. */
public interface LibraryCoverPreparationApi {
    Preparation prepare(AssetProcessingTarget target,String externalKey,long sourceAssetId);
    record Preparation(Long operationId,String revision,boolean ready) {}
}
