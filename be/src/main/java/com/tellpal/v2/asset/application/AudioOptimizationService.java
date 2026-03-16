package com.tellpal.v2.asset.application;

import com.tellpal.v2.shared.infrastructure.firebase.FirebaseStorageService;
import org.springframework.stereotype.Service;

@Service
public class AudioOptimizationService {

    public record AudioOptimizationResult(String storagePath, String downloadUrl) {}

    private final FirebaseStorageService firebaseStorageService;

    public AudioOptimizationService(FirebaseStorageService firebaseStorageService) {
        this.firebaseStorageService = firebaseStorageService;
    }

    public AudioOptimizationResult optimizeAudio(byte[] sourceBytes, String storagePath) {
        firebaseStorageService.upload(storagePath, sourceBytes, "audio/mpeg");
        String downloadUrl = firebaseStorageService.getDownloadUrl(storagePath);
        return new AudioOptimizationResult(storagePath, downloadUrl);
    }
}
