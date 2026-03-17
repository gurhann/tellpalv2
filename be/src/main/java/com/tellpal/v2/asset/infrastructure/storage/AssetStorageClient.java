package com.tellpal.v2.asset.infrastructure.storage;

import java.time.Instant;

import com.tellpal.v2.asset.domain.StorageProvider;

public interface AssetStorageClient {

    StorageProvider provider();

    StorageSignedDownloadUrl createSignedDownloadUrl(String objectPath, Instant issuedAt);
}
