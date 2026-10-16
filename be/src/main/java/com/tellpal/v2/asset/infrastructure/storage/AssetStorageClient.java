package com.tellpal.v2.asset.infrastructure.storage;

import java.time.Instant;
import java.io.InputStream;
import java.util.Optional;

import com.tellpal.v2.asset.domain.StorageProvider;

public interface AssetStorageClient {

    StorageProvider provider();

    /** Returns a reading-only client with explicit transport/RPC deadlines, never legacy defaults. */
    default AssetStorageClient withReadingDeadline(long millis) {
        throw new UnsupportedOperationException("Bounded reading storage is required");
    }

    /** Observes an immutable storage version; legacy unversioned clients fail closed. */
    default Optional<VersionedStorageObject> observeVersion(String path) {
        throw new UnsupportedOperationException("Versioned storage is required for reading preparation");
    }

    /** Opens exactly the observed generation, never whichever bytes happen to be current. */
    default InputStream openVersion(VersionedStorageObject object) {
        throw new UnsupportedOperationException("Pinned storage is required for reading preparation");
    }

    /** Atomically creates an absent object or returns the existing immutable version for equality verification. */
    default VersionedStorageObject createImmutable(String path, String mime, byte[] bytes) {
        throw new UnsupportedOperationException("Conditional storage is required for reading preparation");
    }

    StorageSignedDownloadUrl createSignedDownloadUrl(String objectPath, Instant issuedAt);

    default StorageSignedUploadUrl createSignedUploadUrl(String objectPath, String mimeType, Instant issuedAt) {
        throw new UnsupportedOperationException("Signed upload URLs are not supported for provider " + provider());
    }

    default void uploadObject(String objectPath, String mimeType, long byteSize, InputStream content) {
        throw new UnsupportedOperationException("Backend uploads are not supported for provider " + provider());
    }

    default Optional<StorageObjectMetadata> findObjectMetadata(String objectPath) {
        return Optional.empty();
    }

    default Optional<StorageObjectContent> openObject(String objectPath, StorageObjectRange range) {
        return Optional.empty();
    }
}
