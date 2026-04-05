package com.tellpal.v2.asset.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tellpal.v2.asset.api.AssetKind;
import com.tellpal.v2.asset.api.AssetUploadRequest;
import com.tellpal.v2.asset.api.AssetRecord;
import com.tellpal.v2.asset.api.AssetRegistryApi;
import com.tellpal.v2.asset.api.AssetStorageLocation;
import com.tellpal.v2.asset.api.CompleteMediaAssetUploadCommand;
import com.tellpal.v2.asset.api.InitiateMediaAssetUploadCommand;
import com.tellpal.v2.asset.api.RefreshMediaAssetDownloadUrlCommand;
import com.tellpal.v2.asset.api.RegisterMediaAssetCommand;
import com.tellpal.v2.asset.api.UpdateMediaAssetMetadataCommand;
import com.tellpal.v2.asset.domain.MediaAsset;
import com.tellpal.v2.asset.domain.MediaAssetRepository;
import com.tellpal.v2.asset.domain.MediaKind;
import com.tellpal.v2.asset.domain.StorageProvider;
import com.tellpal.v2.asset.infrastructure.storage.AssetStorageObjectPathBuilder;
import com.tellpal.v2.asset.infrastructure.storage.AssetStorageClientRegistry;
import com.tellpal.v2.asset.infrastructure.storage.AssetUploadTokenClaims;
import com.tellpal.v2.asset.infrastructure.storage.AssetUploadTokenService;
import com.tellpal.v2.asset.infrastructure.storage.StorageObjectMetadata;
import com.tellpal.v2.asset.infrastructure.storage.StorageSignedDownloadUrl;
import com.tellpal.v2.asset.infrastructure.storage.StorageSignedUploadUrl;

/**
 * Application service for registering assets and maintaining mutable storage-backed metadata.
 */
@Service
public class AssetRegistryService implements AssetRegistryApi {

    private final Clock clock;
    private final MediaAssetRepository mediaAssetRepository;
    private final AssetStorageClientRegistry assetStorageClientRegistry;
    private final AssetStorageObjectPathBuilder assetStorageObjectPathBuilder;
    private final AssetUploadTokenService assetUploadTokenService;

    public AssetRegistryService(
            Clock clock,
            MediaAssetRepository mediaAssetRepository,
            AssetStorageClientRegistry assetStorageClientRegistry,
            AssetStorageObjectPathBuilder assetStorageObjectPathBuilder,
            AssetUploadTokenService assetUploadTokenService) {
        this.clock = clock;
        this.mediaAssetRepository = mediaAssetRepository;
        this.assetStorageClientRegistry = assetStorageClientRegistry;
        this.assetStorageObjectPathBuilder = assetStorageObjectPathBuilder;
        this.assetUploadTokenService = assetUploadTokenService;
    }

    /**
     * Creates a signed browser upload request for original Firebase-backed assets.
     */
    @Override
    @Transactional(readOnly = true)
    public AssetUploadRequest initiateUpload(InitiateMediaAssetUploadCommand command) {
        AssetKind uploadKind = requireSupportedUploadKind(command.kind());
        String mimeType = normalizeUploadMimeType(command.mimeType());
        validateUploadMimeType(uploadKind, mimeType);
        Instant issuedAt = Instant.now(clock);
        String objectPath = assetStorageObjectPathBuilder.manualUploadPath(uploadKind, command.fileName(), issuedAt);
        StorageSignedUploadUrl signedUploadUrl = assetStorageClientRegistry.createSignedUploadUrl(
                StorageProvider.FIREBASE_STORAGE,
                objectPath,
                mimeType,
                issuedAt);
        Instant expiresAt = signedUploadUrl.expiresAt();
        String uploadToken = assetUploadTokenService.issue(
                StorageProvider.FIREBASE_STORAGE,
                objectPath,
                AssetApiMapper.toDomain(uploadKind),
                mimeType,
                command.byteSize(),
                issuedAt,
                expiresAt);
        return new AssetUploadRequest(
                com.tellpal.v2.asset.api.AssetStorageProvider.FIREBASE_STORAGE,
                objectPath,
                signedUploadUrl.url(),
                signedUploadUrl.httpMethod(),
                signedUploadUrl.requiredHeaders(),
                expiresAt,
                uploadToken);
    }

    /**
     * Finalizes a browser upload by verifying the object in storage and registering or reusing the
     * media asset row.
     */
    @Override
    @Transactional
    public AssetRecord completeUpload(CompleteMediaAssetUploadCommand command) {
        AssetUploadTokenClaims uploadClaims = decodeUploadClaims(command.uploadToken());
        String objectPath = requireManagedFirebasePath(uploadClaims.objectPath());
        StorageProvider provider = uploadClaims.provider();
        StorageObjectMetadata objectMetadata = assetStorageClientRegistry.findObjectMetadata(provider, objectPath)
                .orElseThrow(() -> new MediaAssetUploadObjectNotFoundException(objectPath));
        validateUploadedObject(uploadClaims, objectMetadata);

        Optional<MediaAsset> existingAsset = mediaAssetRepository.findByProviderAndObjectPath(provider, objectPath);
        if (existingAsset.isPresent()) {
            MediaAsset mediaAsset = existingAsset.get();
            mediaAsset.updateMetadata(
                    objectMetadata.mimeType(),
                    objectMetadata.byteSize(),
                    command.checksumSha256());
            return AssetApiMapper.toRecord(mediaAssetRepository.save(mediaAsset));
        }

        MediaAsset mediaAsset = MediaAsset.register(provider, objectPath, uploadClaims.kind());
        mediaAsset.updateMetadata(
                objectMetadata.mimeType(),
                objectMetadata.byteSize(),
                command.checksumSha256());
        return AssetApiMapper.toRecord(mediaAssetRepository.save(mediaAsset));
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

    private AssetUploadTokenClaims decodeUploadClaims(String uploadToken) {
        try {
            return assetUploadTokenService.verifyAndDecode(uploadToken);
        } catch (IllegalArgumentException exception) {
            throw new MediaAssetUploadTokenInvalidException("Asset upload token is invalid", exception);
        }
    }

    private String requireManagedFirebasePath(String objectPath) {
        try {
            return assetStorageObjectPathBuilder.requireManagedFirebasePath(objectPath);
        } catch (IllegalArgumentException exception) {
            throw new MediaAssetUploadTokenInvalidException("Asset upload token points outside the managed Firebase prefix", exception);
        }
    }

    private static AssetKind requireSupportedUploadKind(AssetKind kind) {
        if (kind == AssetKind.ORIGINAL_IMAGE || kind == AssetKind.ORIGINAL_AUDIO) {
            return kind;
        }
        throw new IllegalArgumentException("Direct uploads support only ORIGINAL_IMAGE and ORIGINAL_AUDIO");
    }

    private static String normalizeUploadMimeType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            throw new IllegalArgumentException("Upload MIME type must not be blank");
        }
        return mimeType.trim().toLowerCase(Locale.ROOT);
    }

    private static void validateUploadMimeType(AssetKind kind, String mimeType) {
        boolean matches = switch (kind) {
            case ORIGINAL_IMAGE -> mimeType.startsWith("image/");
            case ORIGINAL_AUDIO -> mimeType.startsWith("audio/");
            default -> false;
        };
        if (!matches) {
            throw new IllegalArgumentException("Upload MIME type does not match the selected asset kind");
        }
    }

    private static void validateUploadedObject(
            AssetUploadTokenClaims uploadClaims,
            StorageObjectMetadata objectMetadata) {
        String storedMimeType = normalizeUploadMimeType(objectMetadata.mimeType());
        if (!storedMimeType.equalsIgnoreCase(uploadClaims.mimeType())) {
            throw new MediaAssetUploadMetadataMismatchException(
                    "Uploaded object MIME type does not match the initiated upload request");
        }
        if (!uploadClaims.provider().equals(StorageProvider.FIREBASE_STORAGE)) {
            throw new MediaAssetUploadTokenInvalidException("Asset upload token provider is invalid");
        }
        if (!objectMetadata.byteSize().equals(uploadClaims.byteSize())) {
            throw new MediaAssetUploadMetadataMismatchException(
                    "Uploaded object byte size does not match the initiated upload request");
        }
    }
}
