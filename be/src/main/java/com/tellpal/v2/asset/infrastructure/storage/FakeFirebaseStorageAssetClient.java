package com.tellpal.v2.asset.infrastructure.storage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
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
    private final ConcurrentMap<String, UploadedObject> uploadedObjects = new ConcurrentHashMap<>();

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
        return Optional.ofNullable(uploadedObjects.get(stripLeadingSlash(objectPath)))
                .map(uploadedObject -> new StorageObjectMetadata(uploadedObject.mimeType(), uploadedObject.byteSize()));
    }

    @Override
    public void uploadObject(String objectPath, String mimeType, long byteSize, InputStream content) {
        try {
            byte[] bytes = content.readAllBytes();
            if (bytes.length != byteSize) {
                throw new IllegalArgumentException("Fake upload byte size does not match the stream content");
            }
            uploadedObjects.put(stripLeadingSlash(objectPath), new UploadedObject(mimeType, bytes));
        } catch (IOException exception) {
            throw new IllegalStateException("Fake Firebase object could not be uploaded", exception);
        }
    }

    public void storeUploadedObject(String objectPath, String mimeType, long byteSize) {
        byte[] bytes = new byte[Math.toIntExact(byteSize)];
        Arrays.fill(bytes, (byte) 'x');
        uploadedObjects.put(stripLeadingSlash(objectPath), new UploadedObject(mimeType, bytes));
    }

    public void clearUploadedObjects() {
        uploadedObjects.clear();
    }

    @Override
    public Optional<StorageObjectContent> openObject(String objectPath, StorageObjectRange range) {
        UploadedObject uploadedObject = uploadedObjects.get(stripLeadingSlash(objectPath));
        if (uploadedObject == null) {
            return Optional.empty();
        }
        byte[] bytes = uploadedObject.content();
        int start = 0;
        int end = bytes.length == 0 ? -1 : bytes.length - 1;
        Long rangeStart = null;
        Long rangeEnd = null;
        if (range != null) {
            start = Math.toIntExact(range.startInclusive());
            end = Math.toIntExact(range.endInclusive());
            rangeStart = range.startInclusive();
            rangeEnd = range.endInclusive();
        }
        byte[] content = end < start ? new byte[0] : Arrays.copyOfRange(bytes, start, end + 1);
        return Optional.of(new StorageObjectContent(
                uploadedObject.mimeType(),
                uploadedObject.byteSize(),
                content.length,
                rangeStart,
                rangeEnd,
                new ByteArrayInputStream(content)));
    }

    private static String stripLeadingSlash(String objectPath) {
        return objectPath.startsWith("/") ? objectPath.substring(1) : objectPath;
    }

    private record UploadedObject(String mimeType, byte[] content) {

        private long byteSize() {
            return content.length;
        }
    }
}
