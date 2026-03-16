package com.tellpal.v2.shared.application;

import com.tellpal.v2.shared.domain.MediaKind;
import com.tellpal.v2.shared.infrastructure.firebase.FirebaseStorageService;
import com.tellpal.v2.shared.infrastructure.persistence.MediaAssetEntity;
import com.tellpal.v2.shared.infrastructure.persistence.MediaAssetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
@Transactional
public class MediaAssetService {

    private final MediaAssetRepository mediaAssetRepository;
    private final FirebaseStorageService firebaseStorageService;

    public MediaAssetService(MediaAssetRepository mediaAssetRepository,
                             FirebaseStorageService firebaseStorageService) {
        this.mediaAssetRepository = mediaAssetRepository;
        this.firebaseStorageService = firebaseStorageService;
    }

    public MediaAssetEntity createMediaAsset(String objectPath, MediaKind kind,
                                             String mimeType, Long bytes, byte[] data) {
        String checksum = calculateChecksum(data);
        firebaseStorageService.upload(objectPath, data, mimeType);
        MediaAssetEntity entity = new MediaAssetEntity(
                "FIREBASE_STORAGE", objectPath, kind, mimeType, bytes, checksum);
        return mediaAssetRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public MediaAssetEntity getMediaAsset(Long id) {
        return mediaAssetRepository.findById(id)
                .orElseThrow(() -> new MediaAssetNotFoundException(id));
    }

    public void deleteMediaAsset(Long id) {
        MediaAssetEntity asset = getMediaAsset(id);
        firebaseStorageService.delete(asset.getObjectPath());
        mediaAssetRepository.delete(asset);
    }

    public MediaAssetEntity updateDownloadUrlCache(Long id) {
        MediaAssetEntity asset = getMediaAsset(id);
        String url = firebaseStorageService.getDownloadUrl(asset.getObjectPath());
        asset.setDownloadUrl(url);
        return mediaAssetRepository.save(asset);
    }

    public String calculateChecksum(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
