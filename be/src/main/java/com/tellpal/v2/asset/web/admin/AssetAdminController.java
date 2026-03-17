package com.tellpal.v2.asset.web.admin;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.tellpal.v2.asset.api.AssetKind;
import com.tellpal.v2.asset.api.AssetRegistryApi;
import com.tellpal.v2.asset.api.AssetStorageProvider;
import com.tellpal.v2.asset.api.RefreshMediaAssetDownloadUrlCommand;
import com.tellpal.v2.asset.api.RegisterMediaAssetCommand;
import com.tellpal.v2.asset.api.UpdateMediaAssetMetadataCommand;
import com.tellpal.v2.asset.application.MediaAssetNotFoundException;
import com.tellpal.v2.shared.web.admin.AdminApiController;

@AdminApiController
@RequestMapping("/api/admin/media")
public class AssetAdminController {

    private final AssetRegistryApi assetRegistryApi;

    public AssetAdminController(AssetRegistryApi assetRegistryApi) {
        this.assetRegistryApi = assetRegistryApi;
    }

    @PostMapping
    public ResponseEntity<AdminAssetResponse> registerAsset(@Valid @RequestBody RegisterMediaAssetRequest request) {
        AdminAssetResponse response = AdminAssetResponse.from(assetRegistryApi.register(request.toCommand()));
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{assetId}")
                .buildAndExpand(response.assetId())
                .toUri())
                .body(response);
    }

    @GetMapping
    public List<AdminAssetResponse> listRecentAssets(
            @RequestParam(name = "limit", defaultValue = "20") @Min(value = 1, message = "limit must be positive")
            int limit) {
        return assetRegistryApi.listRecent(limit).stream()
                .map(AdminAssetResponse::from)
                .toList();
    }

    @GetMapping("/{assetId}")
    public AdminAssetResponse getAsset(@PathVariable Long assetId) {
        return assetRegistryApi.findById(assetId)
                .map(AdminAssetResponse::from)
                .orElseThrow(() -> new MediaAssetNotFoundException(assetId));
    }

    @PutMapping("/{assetId}/metadata")
    public AdminAssetResponse updateAssetMetadata(
            @PathVariable Long assetId,
            @Valid @RequestBody UpdateAssetMetadataRequest request) {
        return AdminAssetResponse.from(assetRegistryApi.updateMetadata(request.toCommand(assetId)));
    }

    @PostMapping("/{assetId}/download-url-cache/refresh")
    public AdminAssetResponse refreshDownloadUrlCache(@PathVariable Long assetId) {
        return AdminAssetResponse.from(assetRegistryApi.refreshDownloadUrlCache(
                new RefreshMediaAssetDownloadUrlCommand(assetId)));
    }
}

record RegisterMediaAssetRequest(
        @NotNull(message = "provider is required")
        AssetStorageProvider provider,
        @NotBlank(message = "objectPath is required")
        String objectPath,
        @NotNull(message = "kind is required")
        AssetKind kind,
        String mimeType,
        @Min(value = 0, message = "byteSize must not be negative")
        Long byteSize,
        String checksumSha256) {

    RegisterMediaAssetCommand toCommand() {
        return new RegisterMediaAssetCommand(provider, objectPath, kind, mimeType, byteSize, checksumSha256);
    }
}

record UpdateAssetMetadataRequest(
        String mimeType,
        @Min(value = 0, message = "byteSize must not be negative")
        Long byteSize,
        String checksumSha256) {

    UpdateMediaAssetMetadataCommand toCommand(Long assetId) {
        return new UpdateMediaAssetMetadataCommand(assetId, mimeType, byteSize, checksumSha256);
    }
}
