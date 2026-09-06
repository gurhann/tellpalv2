package com.tellpal.v2.asset.web.admin;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.tellpal.v2.asset.api.AssetProcessingApi;
import com.tellpal.v2.asset.api.AssetProcessingKind;
import com.tellpal.v2.asset.api.AssetProcessingCommands.RetryAssetProcessingCommand;
import com.tellpal.v2.asset.api.AssetProcessingCommands.ScheduleAssetProcessingCommand;
import com.tellpal.v2.asset.api.AssetProcessingContentType;
import com.tellpal.v2.asset.api.AssetProcessingTarget;
import com.tellpal.v2.asset.api.AssetProcessingTargetScope;
import com.tellpal.v2.asset.application.AssetProcessingApplicationExceptions.AssetProcessingNotFoundException;
import com.tellpal.v2.shared.domain.LanguageCode;
import com.tellpal.v2.shared.web.admin.AdminApiController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@AdminApiController
@RequestMapping("/api/admin/media-processing")
@Tag(name = "Admin Asset Processing", description = "Asset processing scheduling and retry endpoints.")
@SecurityRequirement(name = "adminBearerAuth")
public class AssetProcessingAdminController {

    private final AssetProcessingApi assetProcessingApi;

    public AssetProcessingAdminController(AssetProcessingApi assetProcessingApi) {
        this.assetProcessingApi = assetProcessingApi;
    }

    @PostMapping
    @Operation(summary = "Schedule asset processing", description = "Queues asset processing for a localized or content-scoped target.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Asset processing scheduled"),
            @ApiResponse(responseCode = "400", description = "Schedule request is invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "401", description = "Admin token is missing or invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "403", description = "Admin user lacks permission", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "409", description = "Asset processing is already scheduled or cannot be retried yet", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public ResponseEntity<AdminAssetProcessingResponse> scheduleProcessing(
            @Valid @RequestBody ScheduleAssetProcessingRequest request) {
        return ResponseEntity.accepted()
                .body(AdminAssetProcessingResponse.from(assetProcessingApi.schedule(request.toCommand())));
    }

    @PostMapping("/{contentId}/localizations/{languageCode}/retry")
    @Operation(summary = "Retry asset processing", description = "Reschedules processing for a localized content item after a previous attempt.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Asset processing retry accepted"),
            @ApiResponse(responseCode = "400", description = "Retry request is invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "401", description = "Admin token is missing or invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "403", description = "Admin user lacks permission", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "404", description = "Processing record or localization was not found", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "409", description = "Asset processing is already pending, running, or completed", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public ResponseEntity<AdminAssetProcessingResponse> retryProcessing(
            @PathVariable Long contentId,
            @PathVariable String languageCode,
            @Valid @RequestBody RetryAssetProcessingRequest request) {
        return ResponseEntity.accepted()
                .body(AdminAssetProcessingResponse.from(assetProcessingApi.retry(
                        request.toCommand(contentId, languageCode))));
    }

    @PostMapping("/{contentId}/content/retry")
    @Operation(summary = "Retry content-scoped asset processing", description = "Reschedules one shared content processing job.")
    public ResponseEntity<AdminAssetProcessingResponse> retryContentProcessing(
            @PathVariable Long contentId,
            @Valid @RequestBody RetryAssetProcessingRequest request) {
        return ResponseEntity.accepted()
                .body(AdminAssetProcessingResponse.from(assetProcessingApi.retry(
                        request.toContentCommand(contentId))));
    }

    @GetMapping("/{contentId}/localizations/{languageCode}")
    @Operation(summary = "Get processing status", description = "Returns the current asset processing state for one localized content item.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Asset processing status returned"),
            @ApiResponse(responseCode = "401", description = "Admin token is missing or invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "403", description = "Admin user lacks permission", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "404", description = "Processing record was not found", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public AdminAssetProcessingResponse getProcessingStatus(
            @PathVariable Long contentId,
            @PathVariable String languageCode,
            @RequestParam(name = "kind", defaultValue = "DELIVERY") AssetProcessingKind kind) {
        LanguageCode language = LanguageCode.from(languageCode);
        return assetProcessingApi.findByTarget(AssetProcessingTarget.localization(contentId, language), kind)
                .map(AdminAssetProcessingResponse::from)
                .orElseThrow(() -> new AssetProcessingNotFoundException(contentId, language));
    }

    @GetMapping("/{contentId}/content")
    @Operation(summary = "Get content-scoped processing status", description = "Returns the shared processing state for content.")
    public AdminAssetProcessingResponse getContentProcessingStatus(@PathVariable Long contentId) {
        return assetProcessingApi.findByContent(contentId)
                .map(AdminAssetProcessingResponse::from)
                .orElseThrow(() -> new AssetProcessingNotFoundException(AssetProcessingTarget.content(contentId)));
    }

    @GetMapping
    @Operation(summary = "List recent processing jobs", description = "Returns recent asset processing jobs ordered by recency.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recent processing jobs returned"),
            @ApiResponse(responseCode = "400", description = "List request is invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "401", description = "Admin token is missing or invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "403", description = "Admin user lacks permission", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public List<AdminAssetProcessingResponse> listRecentProcessing(
            @RequestParam(name = "limit", defaultValue = "20") @Min(value = 1, message = "limit must be positive")
            int limit) {
        return assetProcessingApi.listRecent(limit).stream()
                .map(AdminAssetProcessingResponse::from)
                .toList();
    }
}

record ScheduleAssetProcessingRequest(
        @Positive(message = "contentId must be positive")
        Long contentId,
        String languageCode,
        AssetProcessingTargetScope targetScope,
        @NotNull(message = "contentType is required")
        AssetProcessingContentType contentType,
        @NotBlank(message = "externalKey is required")
        String externalKey,
        @Positive(message = "coverSourceAssetId must be positive")
        Long coverSourceAssetId,
        @Positive(message = "audioSourceAssetId must be positive")
        Long audioSourceAssetId,
        @Min(value = 0, message = "pageCount must not be negative")
    Integer pageCount,
        AssetProcessingKind kind) {

    ScheduleAssetProcessingCommand toCommand() {
        AssetProcessingTargetScope resolvedScope = targetScope == null
                ? AssetProcessingTargetScope.LOCALIZATION
                : targetScope;
        if (resolvedScope == AssetProcessingTargetScope.CONTENT) {
            if (languageCode != null && !languageCode.isBlank()) {
                throw new IllegalArgumentException(
                        "Content processing target must not include a language code");
            }
        } else if (languageCode == null || languageCode.isBlank()) {
            throw new IllegalArgumentException(
                    "Localization processing target requires a language code");
        }

        AssetProcessingTarget target = resolvedScope == AssetProcessingTargetScope.CONTENT
                ? AssetProcessingTarget.content(contentId)
                : AssetProcessingTarget.localization(contentId, LanguageCode.from(languageCode));
        return new ScheduleAssetProcessingCommand(
                target,
                kind == null ? AssetProcessingKind.DELIVERY : kind,
                contentType,
                externalKey,
                coverSourceAssetId,
                audioSourceAssetId,
                pageCount);
    }
}

record RetryAssetProcessingRequest(
        @NotNull(message = "contentType is required")
        AssetProcessingContentType contentType,
        @NotBlank(message = "externalKey is required")
        String externalKey,
        @Positive(message = "coverSourceAssetId must be positive")
        Long coverSourceAssetId,
        @Positive(message = "audioSourceAssetId must be positive")
        Long audioSourceAssetId,
        @Min(value = 0, message = "pageCount must not be negative")
        Integer pageCount,
        AssetProcessingKind kind) {

    RetryAssetProcessingCommand toCommand(Long contentId, String languageCode) {
        return new RetryAssetProcessingCommand(
                AssetProcessingTarget.localization(contentId, LanguageCode.from(languageCode)),
                kind == null ? AssetProcessingKind.DELIVERY : kind,
                contentType,
                externalKey,
                coverSourceAssetId,
                audioSourceAssetId,
                pageCount);
    }

    RetryAssetProcessingCommand toContentCommand(Long contentId) {
        return new RetryAssetProcessingCommand(
                AssetProcessingTarget.content(contentId),
                kind == null ? AssetProcessingKind.DELIVERY : kind,
                contentType,
                externalKey,
                coverSourceAssetId,
                audioSourceAssetId,
                pageCount);
    }
}
