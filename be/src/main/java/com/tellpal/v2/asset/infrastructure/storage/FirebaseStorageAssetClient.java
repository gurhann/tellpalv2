package com.tellpal.v2.asset.infrastructure.storage;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.google.cloud.ReadChannel;
import com.google.cloud.WriteChannel;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.HttpMethod;
import com.google.cloud.storage.Storage;

import com.tellpal.v2.asset.domain.StorageProvider;

class FirebaseStorageAssetClient implements AssetStorageClient {

    private final Storage storage;
    private final ServiceAccountCredentials serviceAccountCredentials;
    private final AssetStorageFirebaseProperties properties;

    FirebaseStorageAssetClient(
            Storage storage,
            ServiceAccountCredentials serviceAccountCredentials,
            AssetStorageFirebaseProperties properties) {
        this.storage = storage;
        this.serviceAccountCredentials = serviceAccountCredentials;
        this.properties = properties;
    }

    @Override
    public StorageProvider provider() {
        return StorageProvider.FIREBASE_STORAGE;
    }

    @Override
    public StorageSignedDownloadUrl createSignedDownloadUrl(String objectPath, Instant issuedAt) {
        Instant expiresAt = issuedAt.plus(properties.signedDownloadTtl());
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId(objectPath)).build();
        URL url = storage.signUrl(
                blobInfo,
                properties.signedDownloadTtl().toSeconds(),
                TimeUnit.SECONDS,
                Storage.SignUrlOption.httpMethod(HttpMethod.GET),
                Storage.SignUrlOption.withV4Signature(),
                Storage.SignUrlOption.signWith(serviceAccountCredentials));
        return new StorageSignedDownloadUrl(url.toString(), expiresAt);
    }

    @Override
    public StorageSignedUploadUrl createSignedUploadUrl(String objectPath, String mimeType, Instant issuedAt) {
        Instant expiresAt = issuedAt.plus(properties.signedUploadTtl());
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId(objectPath))
                .setContentType(mimeType)
                .build();
        URL url = storage.signUrl(
                blobInfo,
                properties.signedUploadTtl().toSeconds(),
                TimeUnit.SECONDS,
                Storage.SignUrlOption.httpMethod(HttpMethod.PUT),
                Storage.SignUrlOption.withContentType(),
                Storage.SignUrlOption.withV4Signature(),
                Storage.SignUrlOption.signWith(serviceAccountCredentials));
        return new StorageSignedUploadUrl(
                url.toString(),
                "PUT",
                Map.of("Content-Type", mimeType),
                expiresAt);
    }

    @Override
    public void uploadObject(String objectPath, String mimeType, long byteSize, InputStream content) {
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId(objectPath))
                .setContentType(mimeType)
                .build();
        try (WriteChannel writer = storage.writer(blobInfo)) {
            content.transferTo(Channels.newOutputStream(writer));
        } catch (IOException exception) {
            throw new IllegalStateException("Firebase Storage object could not be uploaded", exception);
        }
    }

    @Override
    public Optional<StorageObjectMetadata> findObjectMetadata(String objectPath) {
        Blob blob = storage.get(blobId(objectPath));
        if (blob == null || !blob.exists()) {
            return Optional.empty();
        }
        return Optional.of(new StorageObjectMetadata(blob.getContentType(), blob.getSize()));
    }

    @Override
    public Optional<StorageObjectContent> openObject(String objectPath, StorageObjectRange range) {
        Blob blob = storage.get(blobId(objectPath));
        if (blob == null || !blob.exists()) {
            return Optional.empty();
        }
        ReadChannel reader = blob.reader();
        long byteSize = blob.getSize();
        long contentLength = byteSize;
        Long rangeStart = null;
        Long rangeEnd = null;
        try {
            if (range != null) {
                reader.seek(range.startInclusive());
                reader.limit(range.contentLength());
                contentLength = range.contentLength();
                rangeStart = range.startInclusive();
                rangeEnd = range.endInclusive();
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Firebase Storage object range could not be opened", exception);
        }
        return Optional.of(new StorageObjectContent(
                normalizeMimeType(blob.getContentType()),
                byteSize,
                contentLength,
                rangeStart,
                rangeEnd,
                Channels.newInputStream(reader)));
    }

    @Override
    public AssetStorageClient withReadingDeadline(long millis) {
        int timeout=Math.toIntExact(millis);
        var duration=java.time.Duration.ofMillis(millis);
        var retries=storage.getOptions().getRetrySettings().toBuilder().setMaxAttempts(1)
                .setTotalTimeoutDuration(duration).setInitialRpcTimeoutDuration(duration).setMaxRpcTimeoutDuration(duration).build();
        var transport=com.google.cloud.http.HttpTransportOptions.newBuilder().setConnectTimeout(timeout).setReadTimeout(timeout).build();
        var bounded=storage.getOptions().toBuilder().setRetrySettings(retries).setTransportOptions(transport).build().getService();
        return new FirebaseStorageAssetClient(bounded,serviceAccountCredentials,properties);
    }

    @Override
    public Optional<VersionedStorageObject> observeVersion(String path) {
        Blob blob = storage.get(blobId(path));
        return blob == null ? Optional.empty() : Optional.of(version(path, blob));
    }

    @Override
    public InputStream openVersion(VersionedStorageObject object) {
        BlobId pinned = BlobId.of(properties.bucketName(), stripLeadingSlash(object.path()), Long.valueOf(object.version()));
        Blob blob=storage.get(pinned);
        if(blob==null || !version(object.path(),blob).equals(object)) throw new IllegalStateException("PINNED_STORAGE_METADATA_CHANGED");
        ReadChannel reader=storage.reader(pinned, Storage.BlobSourceOption.generationMatch());
        reader.setChunkSize(1024*1024);
        return Channels.newInputStream(reader);
    }

    @Override
    public VersionedStorageObject createImmutable(String path, String mime, byte[] bytes) {
        try {
            Blob blob = storage.create(BlobInfo.newBuilder(blobId(path)).setContentType(mime).build(), bytes,
                    Storage.BlobTargetOption.doesNotExist());
            return version(path, blob);
        } catch (com.google.cloud.storage.StorageException conflict) {
            if (conflict.getCode() != 412) throw conflict;
            return observeVersion(path).orElseThrow(() -> new IllegalStateException("Immutable conflict object disappeared"));
        }
    }

    private static VersionedStorageObject version(String path, Blob blob) {
        return new VersionedStorageObject(path, blob.getGeneration().toString(), normalizeMimeType(blob.getContentType()), blob.getSize());
    }

    private BlobId blobId(String objectPath) {
        return BlobId.of(properties.bucketName(), stripLeadingSlash(objectPath));
    }

    private static String stripLeadingSlash(String objectPath) {
        return objectPath.startsWith("/") ? objectPath.substring(1) : objectPath;
    }

    private static String normalizeMimeType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return "application/octet-stream";
        }
        return mimeType.trim();
    }
}
