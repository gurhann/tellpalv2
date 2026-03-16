package com.tellpal.v2.presentation.api.admin;

import com.tellpal.v2.shared.application.MediaAssetService;
import com.tellpal.v2.shared.domain.MediaKind;
import com.tellpal.v2.shared.infrastructure.persistence.MediaAssetEntity;
import com.tellpal.v2.presentation.dto.media.MediaAssetResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/media")
public class MediaAdminController {

    private final MediaAssetService mediaAssetService;

    public MediaAdminController(MediaAssetService mediaAssetService) {
        this.mediaAssetService = mediaAssetService;
    }

    @PostMapping("/upload")
    public ResponseEntity<MediaAssetResponse> uploadMedia(@RequestParam("file") MultipartFile file,
                                                           @RequestParam String kind) throws IOException {
        String objectPath = "uploads/" + kind.toLowerCase() + "/" + UUID.randomUUID() + "/" + file.getOriginalFilename();
        MediaAssetEntity entity = mediaAssetService.createMediaAsset(
                objectPath,
                MediaKind.valueOf(kind),
                file.getContentType(),
                file.getSize(),
                file.getBytes());
        return ResponseEntity.status(201).body(toMediaAssetResponse(entity));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MediaAssetResponse> getMediaAsset(@PathVariable Long id) {
        MediaAssetEntity entity = mediaAssetService.getMediaAsset(id);
        return ResponseEntity.ok(toMediaAssetResponse(entity));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMediaAsset(@PathVariable Long id) {
        mediaAssetService.deleteMediaAsset(id);
        return ResponseEntity.noContent().build();
    }

    private MediaAssetResponse toMediaAssetResponse(MediaAssetEntity e) {
        return new MediaAssetResponse(
                e.getId(),
                e.getProvider(),
                e.getObjectPath(),
                e.getKind().name(),
                e.getMimeType(),
                e.getBytes(),
                e.getChecksumSha256(),
                e.getDownloadUrl());
    }
}
