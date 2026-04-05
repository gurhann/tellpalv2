package com.tellpal.v2.asset.infrastructure.storage;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.tellpal.v2.asset.domain.StorageProvider;

@Component
public class AssetStorageClientRegistry {

    private final Map<StorageProvider, AssetStorageClient> clients;

    public AssetStorageClientRegistry(List<AssetStorageClient> clients) {
        this.clients = new EnumMap<>(StorageProvider.class);
        for (AssetStorageClient client : clients) {
            AssetStorageClient previous = this.clients.put(client.provider(), client);
            if (previous != null) {
                throw new IllegalStateException("Duplicate asset storage client for provider " + client.provider());
            }
        }
    }

    public StorageSignedDownloadUrl createSignedDownloadUrl(
            StorageProvider provider,
            String objectPath,
            Instant issuedAt) {
        return clientFor(provider).createSignedDownloadUrl(requireObjectPath(objectPath), requireIssuedAt(issuedAt));
    }

    public StorageSignedUploadUrl createSignedUploadUrl(
            StorageProvider provider,
            String objectPath,
            String mimeType,
            Instant issuedAt) {
        if (mimeType == null || mimeType.isBlank()) {
            throw new IllegalArgumentException("Signed upload MIME type must not be blank");
        }
        return clientFor(provider).createSignedUploadUrl(
                requireObjectPath(objectPath),
                mimeType.trim(),
                requireIssuedAt(issuedAt));
    }

    public Optional<StorageObjectMetadata> findObjectMetadata(StorageProvider provider, String objectPath) {
        return clientFor(provider).findObjectMetadata(requireObjectPath(objectPath));
    }

    private AssetStorageClient clientFor(StorageProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("Storage provider must not be null");
        }
        AssetStorageClient client = clients.get(provider);
        if (client == null) {
            throw new IllegalStateException("No asset storage client registered for provider " + provider);
        }
        return client;
    }

    private static String requireObjectPath(String objectPath) {
        if (objectPath == null || objectPath.isBlank()) {
            throw new IllegalArgumentException("Storage object path must not be blank");
        }
        return objectPath.trim();
    }

    private static Instant requireIssuedAt(Instant issuedAt) {
        if (issuedAt == null) {
            throw new IllegalArgumentException("Signed URL issue time must not be null");
        }
        return issuedAt;
    }
}
