package com.tellpal.v2.asset.infrastructure.storage;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.web.util.UriUtils;

import com.tellpal.v2.asset.domain.StorageProvider;

/**
 * Test-only Firebase storage client that keeps uploaded object metadata in memory.
 */
public class FakeFirebaseStorageAssetClient implements AssetStorageClient {

    private static final String BASE_URL = "https://firebase-storage.test";

    private final AssetStorageFirebaseProperties properties;
    private final ConcurrentMap<String, StorageObjectMetadata> uploadedObjects = new ConcurrentHashMap<>();

    public FakeFirebaseStorageAssetClient(AssetStorageFirebaseProperties properties) {
        this.properties = properties;
    }

    @Override
    public StorageProvider provider() {
        return StorageProvider.FIREBASE_STORAGE;
    }

    @Override
    public StorageSignedDownloadUrl createSignedDownloadUrl(String objectPath, Instant issuedAt) {
        String encodedPath = UriUtils.encodePath(stripLeadingSlash(objectPath), StandardCharsets.UTF_8);
        Instant expiresAt = issuedAt.plus(properties.signedDownloadTtl());
        String url = BASE_URL + "/download/" + properties.bucketName() + "/" + encodedPath + "?expiresAt=" + expiresAt;
        return new StorageSignedDownloadUrl(url, expiresAt);
    }

    @Override
    public StorageSignedUploadUrl createSignedUploadUrl(String objectPath, String mimeType, Instant issuedAt) {
        String encodedPath = UriUtils.encodePath(stripLeadingSlash(objectPath), StandardCharsets.UTF_8);
        Instant expiresAt = issuedAt.plus(properties.signedUploadTtl());
        String url = BASE_URL + "/upload/" + properties.bucketName() + "/" + encodedPath + "?expiresAt=" + expiresAt;
        return new StorageSignedUploadUrl(
                url,
                "PUT",
                Map.of("Content-Type", mimeType),
                expiresAt);
    }

    @Override
    public Optional<StorageObjectMetadata> findObjectMetadata(String objectPath) {
        return Optional.ofNullable(uploadedObjects.get(stripLeadingSlash(objectPath)));
    }

    public void storeUploadedObject(String objectPath, String mimeType, long byteSize) {
        uploadedObjects.put(stripLeadingSlash(objectPath), new StorageObjectMetadata(mimeType, byteSize));
    }

    public void clearUploadedObjects() {
        uploadedObjects.clear();
    }

    private static String stripLeadingSlash(String objectPath) {
        return objectPath.startsWith("/") ? objectPath.substring(1) : objectPath;
    }
}
