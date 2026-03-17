package com.tellpal.v2.asset.application;

import java.time.Clock;
import java.time.Instant;
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

    @Override
    @Transactional(readOnly = true)
    public Optional<AssetRecord> findById(Long assetId) {
        return mediaAssetRepository.findById(requireAssetId(assetId))
                .map(AssetApiMapper::toRecord);
    }

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

    @Override
    @Transactional
    public AssetRecord updateMetadata(UpdateMediaAssetMetadataCommand command) {
        MediaAsset mediaAsset = loadMediaAsset(command.assetId());
        mediaAsset.updateMetadata(command.mimeType(), command.byteSize(), command.checksumSha256());
        return AssetApiMapper.toRecord(mediaAssetRepository.save(mediaAsset));
    }

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
}
