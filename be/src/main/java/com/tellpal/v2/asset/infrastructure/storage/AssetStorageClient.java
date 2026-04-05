package com.tellpal.v2.asset.infrastructure.storage;

import java.time.Instant;
import java.util.Optional;

import com.tellpal.v2.asset.domain.StorageProvider;

public interface AssetStorageClient {

    StorageProvider provider();

    StorageSignedDownloadUrl createSignedDownloadUrl(String objectPath, Instant issuedAt);

    default StorageSignedUploadUrl createSignedUploadUrl(String objectPath, String mimeType, Instant issuedAt) {
        throw new UnsupportedOperationException("Signed upload URLs are not supported for provider " + provider());
    }

    default Optional<StorageObjectMetadata> findObjectMetadata(String objectPath) {
        return Optional.empty();
    }
}
