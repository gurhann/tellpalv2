package com.tellpal.v2.asset.web.admin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.tellpal.v2.asset.api.AssetContent;
import com.tellpal.v2.asset.api.AssetContentAccessToken;
import com.tellpal.v2.asset.api.AssetContentRange;
import com.tellpal.v2.asset.api.AssetKind;
import com.tellpal.v2.asset.api.AssetUploadRequest;
import com.tellpal.v2.asset.api.AssetRegistryApi;
import com.tellpal.v2.asset.api.AssetStorageProvider;
import com.tellpal.v2.asset.api.BackendMediaAssetUploadCommand;
import com.tellpal.v2.asset.api.CompleteMediaAssetUploadCommand;
import com.tellpal.v2.asset.api.InitiateMediaAssetUploadCommand;
import com.tellpal.v2.asset.api.ProxyMediaAssetUploadCommand;
import com.tellpal.v2.asset.api.RefreshMediaAssetDownloadUrlCommand;
import com.tellpal.v2.asset.api.RegisterMediaAssetCommand;
import com.tellpal.v2.asset.api.UpdateMediaAssetMetadataCommand;
import com.tellpal.v2.asset.application.MediaAssetContentRangeNotSatisfiableException;
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

    @PostMapping(value = "/uploads", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Initiate a direct media upload",
            description = "Deprecated compatibility endpoint. Creates a Firebase Storage signed upload request for an original image or audio asset.",
            deprecated = true)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Signed upload request created"),
            @ApiResponse(responseCode = "400", description = "Upload initiation request is invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "401", description = "Admin token is missing or invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "403", description = "Admin user lacks permission", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public AdminAssetUploadResponse initiateUpload(@Valid @RequestBody InitiateAssetUploadRequest request) {
        return AdminAssetUploadResponse.from(assetRegistryApi.initiateUpload(request.toCommand()));
    }

    @PostMapping(value = "/uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload media through the backend",
            description = "Uploads an original image or audio asset through the backend so browsers do not contact Firebase Storage directly.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Uploaded media asset finalized"),
            @ApiResponse(responseCode = "400", description = "Upload request is invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "401", description = "Admin token is missing or invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "403", description = "Admin user lacks permission", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "409", description = "Uploaded object metadata does not match the upload request", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public AdminAssetResponse uploadFromBackend(
            @RequestParam("kind") AssetKind kind,
            @RequestParam(value = "checksumSha256", required = false) String checksumSha256,
            @RequestParam("file") MultipartFile file) throws IOException {
        return AdminAssetResponse.from(assetRegistryApi.uploadFromBackend(new BackendMediaAssetUploadCommand(
                kind,
                originalFileName(file),
                contentType(file),
                file.getSize(),
                file.getInputStream(),
                checksumSha256)));
    }

    @PostMapping("/uploads/complete")
    @Operation(
            summary = "Complete a direct media upload",
            description = "Deprecated compatibility endpoint. Validates one uploaded Firebase Storage object and registers or reuses the media asset record.",
            deprecated = true)
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

    @PostMapping(value = "/uploads/proxy", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload media through the backend",
            description = "Deprecated compatibility endpoint. Fallback upload path for older signed-upload clients.",
            deprecated = true)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Uploaded media asset finalized"),
            @ApiResponse(responseCode = "400", description = "Upload request is invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "401", description = "Admin token is missing or invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "403", description = "Admin user lacks permission", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "409", description = "Uploaded object metadata does not match the initiated upload request", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public AdminAssetResponse proxyUpload(
            @RequestParam("uploadToken") String uploadToken,
            @RequestParam(value = "checksumSha256", required = false) String checksumSha256,
            @RequestParam("file") MultipartFile file) throws IOException {
        return AdminAssetResponse.from(assetRegistryApi.proxyUpload(new ProxyMediaAssetUploadCommand(
                uploadToken,
                contentType(file),
                file.getSize(),
                file.getInputStream(),
                checksumSha256)));
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
    @Operation(
            summary = "Refresh cached download URL",
            description = "Deprecated compatibility endpoint. Refreshes the cached signed download URL for one media asset.",
            deprecated = true)
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

    @PostMapping("/{assetId}/content-token")
    @Operation(
            summary = "Issue media content token",
            description = "Creates a short-lived backend preview URL for one CMS media asset.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Content token created"),
            @ApiResponse(responseCode = "400", description = "Asset cannot be previewed", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "401", description = "Admin token is missing or invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "403", description = "Admin user lacks permission", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "404", description = "Media asset was not found", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public AdminAssetContentTokenResponse issueContentToken(@PathVariable Long assetId) {
        AssetContentAccessToken token = assetRegistryApi.issueContentAccessToken(assetId);
        String previewUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/admin/media/{assetId}/content")
                .queryParam("token", token.token())
                .buildAndExpand(assetId)
                .toUriString();
        return new AdminAssetContentTokenResponse(previewUrl, token.expiresAt());
    }

    @RequestMapping(value = "/{assetId}/content", method = {RequestMethod.GET, RequestMethod.HEAD})
    @Operation(
            summary = "Stream media content",
            description = "Streams CMS media through the backend using a short-lived content token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Full media content returned"),
            @ApiResponse(responseCode = "206", description = "Partial media content returned"),
            @ApiResponse(responseCode = "400", description = "Content token is invalid or the asset cannot be previewed", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "404", description = "Media asset content was not found", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "416", description = "Requested range cannot be served", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public ResponseEntity<Object> streamContent(
            @PathVariable Long assetId,
            @RequestParam("token") String token,
            @RequestHeader(name = HttpHeaders.RANGE, required = false) String rangeHeader,
            HttpServletRequest request) throws IOException {
        AssetContent content = assetRegistryApi.openContent(assetId, token, parseRangeHeader(rangeHeader));
        HttpHeaders headers = contentHeaders(content);
        HttpStatus status = content.isPartial() ? HttpStatus.PARTIAL_CONTENT : HttpStatus.OK;
        if ("HEAD".equalsIgnoreCase(request.getMethod())) {
            content.content().close();
            return new ResponseEntity<>(headers, status);
        }
        return new ResponseEntity<>(new InputStreamResource(content.content()), headers, status);
    }

    private static HttpHeaders contentHeaders(AssetContent content) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(content.mimeType()));
        headers.setContentLength(content.contentLength());
        headers.setCacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePrivate());
        headers.set("Accept-Ranges", "bytes");
        headers.setContentDisposition(ContentDisposition.inline()
                .filename(content.fileName(), StandardCharsets.UTF_8)
                .build());
        if (content.isPartial()) {
            headers.set(
                    HttpHeaders.CONTENT_RANGE,
                    "bytes %d-%d/%d".formatted(
                            content.rangeStartInclusive(),
                            content.rangeEndInclusive(),
                            content.byteSize()));
        }
        return headers;
    }

    private static AssetContentRange parseRangeHeader(String rangeHeader) {
        if (rangeHeader == null || rangeHeader.isBlank()) {
            return null;
        }
        String normalized = rangeHeader.trim();
        if (!normalized.startsWith("bytes=") || normalized.contains(",")) {
            throw new MediaAssetContentRangeNotSatisfiableException("Only a single bytes range is supported");
        }
        String rangeValue = normalized.substring("bytes=".length());
        int dashIndex = rangeValue.indexOf('-');
        if (dashIndex < 0) {
            throw new MediaAssetContentRangeNotSatisfiableException("Range header is malformed");
        }
        String startValue = rangeValue.substring(0, dashIndex).trim();
        String endValue = rangeValue.substring(dashIndex + 1).trim();
        try {
            if (startValue.isEmpty()) {
                return AssetContentRange.suffix(Long.parseLong(endValue));
            }
            Long endInclusive = endValue.isEmpty() ? null : Long.parseLong(endValue);
            return AssetContentRange.fromStart(Long.parseLong(startValue), endInclusive);
        } catch (RuntimeException exception) {
            throw new MediaAssetContentRangeNotSatisfiableException("Range header is malformed");
        }
    }

    private static String originalFileName(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            return file.getName();
        }
        return originalFilename;
    }

    private static String contentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException("Upload MIME type must not be blank");
        }
        return contentType;
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

record AdminAssetContentTokenResponse(
        String previewUrl,
        Instant expiresAt) {
}
