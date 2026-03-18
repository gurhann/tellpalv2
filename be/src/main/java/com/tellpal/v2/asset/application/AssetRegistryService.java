package com.tellpal.v2.asset.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tellpal.v2.asset.api.AssetRecord;
import com.tellpal.v2.asset.api.AssetRegistryApi;
import com.tellpal.v2.asset.api.AssetStorageLocation;
import com.tellpal.v2.asset.api.RefreshMediaAssetDownloadUrlCommand;
import com.tellpal.v2.asset.api.RegisterMediaAssetCommand;
import com.tellpal.v2.asset.api.UpdateMediaAssetMetadataCommand;
import com.tellpal.v2.asset.domain.MediaAsset;
import com.tellpal.v2.asset.domain.MediaAssetRepository;
import com.tellpal.v2.asset.domain.MediaKind;
import com.tellpal.v2.asset.domain.StorageProvider;
import com.tellpal.v2.asset.infrastructure.storage.AssetStorageClientRegistry;
import com.tellpal.v2.asset.infrastructure.storage.StorageSignedDownloadUrl;

/**
 * Application service for registering assets and maintaining mutable storage-backed metadata.
 */
@Service
public class AssetRegistryService implements AssetRegistryApi {

    private final Clock clock;
    private final MediaAssetRepository mediaAssetRepository;
    private final AssetStorageClientRegistry assetStorageClientRegistry;

    public AssetRegistryService(
            Clock clock,
            MediaAssetRepository mediaAssetRepository,
            AssetStorageClientRegistry assetStorageClientRegistry) {
        this.clock = clock;
        this.mediaAssetRepository = mediaAssetRepository;
        this.assetStorageClientRegistry = assetStorageClientRegistry;
    }

    /**
     * Registers a new asset for a unique storage location.
     *
     * <p>Registration fails if the same provider and object path are already known.
     */
    @Override
    @Transactional
    public AssetRecord register(RegisterMediaAssetCommand command) {
        StorageProvider provider = AssetApiMapper.toDomain(command.provider());
        String objectPath = command.objectPath().trim();
        if (mediaAssetRepository.existsByProviderAndObjectPath(provider, objectPath)) {
            throw new MediaAssetAlreadyExistsException(provider, objectPath);
        }

        MediaKind kind = AssetApiMapper.toDomain(command.kind());
        MediaAsset mediaAsset = MediaAsset.register(provider, objectPath, kind);
        mediaAsset.updateMetadata(command.mimeType(), command.byteSize(), command.checksumSha256());
        return AssetApiMapper.toRecord(mediaAssetRepository.save(mediaAsset));
    }

    /**
     * Returns recent assets for operational read use cases.
     */
    @Override
    @Transactional(readOnly = true)
    public List<AssetRecord> listRecent(int limit) {
        int sanitizedLimit = sanitizeLimit(limit);
        return mediaAssetRepository.findRecent(sanitizedLimit).stream()
                .map(AssetApiMapper::toRecord)
                .toList();
    }

    /**
     * Finds a registered asset by persistent identifier.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<AssetRecord> findById(Long assetId) {
        return mediaAssetRepository.findById(requireAssetId(assetId))
                .map(AssetApiMapper::toRecord);
    }

    /**
     * Finds a registered asset by provider and object path.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<AssetRecord> findByStorageLocation(AssetStorageLocation storageLocation) {
        if (storageLocation == null) {
            throw new IllegalArgumentException("Asset storage location must not be null");
        }
        return mediaAssetRepository.findByProviderAndObjectPath(
                        AssetApiMapper.toDomain(storageLocation.provider()),
                        storageLocation.objectPath())
                .map(AssetApiMapper::toRecord);
    }

    /**
     * Replaces mutable metadata such as MIME type, byte size, and checksum for an existing asset.
     */
    @Override
    @Transactional
    public AssetRecord updateMetadata(UpdateMediaAssetMetadataCommand command) {
        MediaAsset mediaAsset = loadMediaAsset(command.assetId());
        mediaAsset.updateMetadata(command.mimeType(), command.byteSize(), command.checksumSha256());
        return AssetApiMapper.toRecord(mediaAssetRepository.save(mediaAsset));
    }

    /**
     * Refreshes the cached signed download URL using the storage client for the asset provider.
     */
    @Override
    @Transactional
    public AssetRecord refreshDownloadUrlCache(RefreshMediaAssetDownloadUrlCommand command) {
        MediaAsset mediaAsset = loadMediaAsset(command.assetId());
        Instant issuedAt = Instant.now(clock);
        StorageSignedDownloadUrl signedDownloadUrl = assetStorageClientRegistry.createSignedDownloadUrl(
                mediaAsset.getProvider(),
                mediaAsset.getObjectPath(),
                issuedAt);
        mediaAsset.updateDownloadUrlCache(
                signedDownloadUrl.url(),
                issuedAt,
                signedDownloadUrl.expiresAt());
        return AssetApiMapper.toRecord(mediaAssetRepository.save(mediaAsset));
    }

    private MediaAsset loadMediaAsset(Long assetId) {
        return mediaAssetRepository.findById(requireAssetId(assetId))
                .orElseThrow(() -> new MediaAssetNotFoundException(assetId));
    }

    private static Long requireAssetId(Long assetId) {
        if (assetId == null || assetId <= 0) {
            throw new IllegalArgumentException("Asset ID must be positive");
        }
        return assetId;
    }

    private static int sanitizeLimit(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Asset list limit must be positive");
        }
        return Math.min(limit, 100);
    }
}
