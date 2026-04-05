package com.tellpal.v2.asset.web.admin;

import java.util.List;
import java.time.Instant;

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
import com.tellpal.v2.asset.api.AssetUploadRequest;
import com.tellpal.v2.asset.api.AssetRegistryApi;
import com.tellpal.v2.asset.api.AssetStorageProvider;
import com.tellpal.v2.asset.api.CompleteMediaAssetUploadCommand;
import com.tellpal.v2.asset.api.InitiateMediaAssetUploadCommand;
import com.tellpal.v2.asset.api.RefreshMediaAssetDownloadUrlCommand;
import com.tellpal.v2.asset.api.RegisterMediaAssetCommand;
import com.tellpal.v2.asset.api.UpdateMediaAssetMetadataCommand;
import com.tellpal.v2.asset.application.MediaAssetNotFoundException;
import com.tellpal.v2.shared.web.admin.AdminApiController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@AdminApiController
@RequestMapping("/api/admin/media")
@Tag(name = "Admin Assets", description = "Media asset registration and metadata management endpoints.")
@SecurityRequirement(name = "adminBearerAuth")
public class AssetAdminController {

    private final AssetRegistryApi assetRegistryApi;

    public AssetAdminController(AssetRegistryApi assetRegistryApi) {
        this.assetRegistryApi = assetRegistryApi;
    }

    @PostMapping("/uploads")
    @Operation(
            summary = "Initiate a direct media upload",
            description = "Creates a Firebase Storage signed upload request for an original image or audio asset.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Signed upload request created"),
            @ApiResponse(responseCode = "400", description = "Upload initiation request is invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "401", description = "Admin token is missing or invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "403", description = "Admin user lacks permission", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public AdminAssetUploadResponse initiateUpload(@Valid @RequestBody InitiateAssetUploadRequest request) {
        return AdminAssetUploadResponse.from(assetRegistryApi.initiateUpload(request.toCommand()));
    }

    @PostMapping("/uploads/complete")
    @Operation(
            summary = "Complete a direct media upload",
            description = "Validates one uploaded Firebase Storage object and registers or reuses the media asset record.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Uploaded media asset finalized"),
            @ApiResponse(responseCode = "400", description = "Upload completion request is invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "401", description = "Admin token is missing or invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "403", description = "Admin user lacks permission", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "404", description = "Uploaded object was not found", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "409", description = "Uploaded object metadata does not match the signed upload request", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public AdminAssetResponse completeUpload(@Valid @RequestBody CompleteAssetUploadRequest request) {
        return AdminAssetResponse.from(assetRegistryApi.completeUpload(request.toCommand()));
    }

    @PostMapping
    @Operation(summary = "Register a media asset", description = "Creates a media asset record for an uploaded object.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Media asset registered"),
            @ApiResponse(responseCode = "400", description = "Asset request is invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "401", description = "Admin token is missing or invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "403", description = "Admin user lacks permission", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "409", description = "Media asset already exists", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public ResponseEntity<AdminAssetResponse> registerAsset(@Valid @RequestBody RegisterMediaAssetRequest request) {
        AdminAssetResponse response = AdminAssetResponse.from(assetRegistryApi.register(request.toCommand()));
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{assetId}")
                .buildAndExpand(response.assetId())
                .toUri())
                .body(response);
    }

    @GetMapping
    @Operation(summary = "List recent media assets", description = "Returns recent media asset records ordered by recency.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recent media assets returned"),
            @ApiResponse(responseCode = "400", description = "List request is invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "401", description = "Admin token is missing or invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "403", description = "Admin user lacks permission", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public List<AdminAssetResponse> listRecentAssets(
            @RequestParam(name = "limit", defaultValue = "20") @Min(value = 1, message = "limit must be positive")
            int limit) {
        return assetRegistryApi.listRecent(limit).stream()
                .map(AdminAssetResponse::from)
                .toList();
    }

    @GetMapping("/{assetId}")
    @Operation(summary = "Get one media asset", description = "Returns the stored metadata for one media asset.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Media asset returned"),
            @ApiResponse(responseCode = "401", description = "Admin token is missing or invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "403", description = "Admin user lacks permission", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "404", description = "Media asset was not found", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public AdminAssetResponse getAsset(@PathVariable Long assetId) {
        return assetRegistryApi.findById(assetId)
                .map(AdminAssetResponse::from)
                .orElseThrow(() -> new MediaAssetNotFoundException(assetId));
    }

    @PutMapping("/{assetId}/metadata")
    @Operation(summary = "Update media asset metadata", description = "Updates mutable metadata fields for one media asset.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Media asset metadata updated"),
            @ApiResponse(responseCode = "400", description = "Metadata request is invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "401", description = "Admin token is missing or invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "403", description = "Admin user lacks permission", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "404", description = "Media asset was not found", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public AdminAssetResponse updateAssetMetadata(
            @PathVariable Long assetId,
            @Valid @RequestBody UpdateAssetMetadataRequest request) {
        return AdminAssetResponse.from(assetRegistryApi.updateMetadata(request.toCommand(assetId)));
    }

    @PostMapping("/{assetId}/download-url-cache/refresh")
    @Operation(summary = "Refresh cached download URL", description = "Refreshes the cached signed download URL for one media asset.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cached download URL refreshed"),
            @ApiResponse(responseCode = "401", description = "Admin token is missing or invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "403", description = "Admin user lacks permission", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "404", description = "Media asset was not found", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
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

record InitiateAssetUploadRequest(
        @NotNull(message = "kind is required")
        AssetKind kind,
        @NotBlank(message = "fileName is required")
        String fileName,
        @NotBlank(message = "mimeType is required")
        String mimeType,
        @NotNull(message = "byteSize is required")
        @Min(value = 1, message = "byteSize must be positive")
        Long byteSize) {

    InitiateMediaAssetUploadCommand toCommand() {
        return new InitiateMediaAssetUploadCommand(kind, fileName, mimeType, byteSize);
    }
}

record CompleteAssetUploadRequest(
        @NotBlank(message = "uploadToken is required")
        String uploadToken,
        String checksumSha256) {

    CompleteMediaAssetUploadCommand toCommand() {
        return new CompleteMediaAssetUploadCommand(uploadToken, checksumSha256);
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

record AdminAssetUploadResponse(
        AssetStorageProvider provider,
        String objectPath,
        String uploadUrl,
        String httpMethod,
        java.util.Map<String, String> requiredHeaders,
        Instant expiresAt,
        String uploadToken) {

    static AdminAssetUploadResponse from(AssetUploadRequest uploadRequest) {
        return new AdminAssetUploadResponse(
                uploadRequest.provider(),
                uploadRequest.objectPath(),
                uploadRequest.uploadUrl(),
                uploadRequest.httpMethod(),
                uploadRequest.requiredHeaders(),
                uploadRequest.expiresAt(),
                uploadRequest.uploadToken());
    }
}
