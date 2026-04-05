package com.tellpal.v2.asset.infrastructure.storage;

import java.net.URL;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

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
    public Optional<StorageObjectMetadata> findObjectMetadata(String objectPath) {
        Blob blob = storage.get(blobId(objectPath));
        if (blob == null || !blob.exists()) {
            return Optional.empty();
        }
        return Optional.of(new StorageObjectMetadata(blob.getContentType(), blob.getSize()));
    }

    private BlobId blobId(String objectPath) {
        return BlobId.of(properties.bucketName(), stripLeadingSlash(objectPath));
    }

    private static String stripLeadingSlash(String objectPath) {
        return objectPath.startsWith("/") ? objectPath.substring(1) : objectPath;
    }
}
