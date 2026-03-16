package com.tellpal.v2.asset.application;

import com.tellpal.v2.shared.infrastructure.firebase.FirebaseStorageService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ImageOptimizationService {

    public record ImageVariantResult(ImageVariant variant, String storagePath, String downloadUrl) {}

    private final FirebaseStorageService firebaseStorageService;

    public ImageOptimizationService(FirebaseStorageService firebaseStorageService) {
        this.firebaseStorageService = firebaseStorageService;
    }

    public List<ImageVariantResult> optimizeImage(byte[] sourceBytes, String basePath) {
        List<ImageVariantResult> results = new ArrayList<>();
        for (ImageVariant variant : ImageVariant.values()) {
            String path = basePath + "/" + variant.suffix() + ".jpg";
            firebaseStorageService.upload(path, sourceBytes, "image/jpeg");
            String downloadUrl = firebaseStorageService.getDownloadUrl(path);
            results.add(new ImageVariantResult(variant, path, downloadUrl));
        }
        return results;
    }
}
