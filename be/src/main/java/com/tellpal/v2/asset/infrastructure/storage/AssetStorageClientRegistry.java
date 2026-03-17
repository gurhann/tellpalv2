package com.tellpal.v2.asset.infrastructure.storage;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

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
        if (provider == null) {
            throw new IllegalArgumentException("Storage provider must not be null");
        }
        if (objectPath == null || objectPath.isBlank()) {
            throw new IllegalArgumentException("Storage object path must not be blank");
        }
        if (issuedAt == null) {
            throw new IllegalArgumentException("Signed download URL issue time must not be null");
        }
        AssetStorageClient client = clients.get(provider);
        if (client == null) {
            throw new IllegalStateException("No asset storage client registered for provider " + provider);
        }
        return client.createSignedDownloadUrl(objectPath.trim(), issuedAt);
    }
}
