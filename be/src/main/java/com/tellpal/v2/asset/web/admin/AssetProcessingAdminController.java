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
import com.tellpal.v2.asset.api.AssetProcessingCommands.RetryAssetProcessingCommand;
import com.tellpal.v2.asset.api.AssetProcessingCommands.ScheduleAssetProcessingCommand;
import com.tellpal.v2.asset.api.AssetProcessingContentType;
import com.tellpal.v2.asset.application.AssetProcessingApplicationExceptions.AssetProcessingNotFoundException;
import com.tellpal.v2.shared.domain.LanguageCode;
import com.tellpal.v2.shared.web.admin.AdminApiController;

@AdminApiController
@RequestMapping("/api/admin/media-processing")
public class AssetProcessingAdminController {

    private final AssetProcessingApi assetProcessingApi;

    public AssetProcessingAdminController(AssetProcessingApi assetProcessingApi) {
        this.assetProcessingApi = assetProcessingApi;
    }

    @PostMapping
    public ResponseEntity<AdminAssetProcessingResponse> scheduleProcessing(
            @Valid @RequestBody ScheduleAssetProcessingRequest request) {
        return ResponseEntity.accepted()
                .body(AdminAssetProcessingResponse.from(assetProcessingApi.schedule(request.toCommand())));
    }

    @PostMapping("/{contentId}/localizations/{languageCode}/retry")
    public ResponseEntity<AdminAssetProcessingResponse> retryProcessing(
            @PathVariable Long contentId,
            @PathVariable String languageCode,
            @Valid @RequestBody RetryAssetProcessingRequest request) {
        return ResponseEntity.accepted()
                .body(AdminAssetProcessingResponse.from(assetProcessingApi.retry(
                        request.toCommand(contentId, languageCode))));
    }

    @GetMapping("/{contentId}/localizations/{languageCode}")
    public AdminAssetProcessingResponse getProcessingStatus(
            @PathVariable Long contentId,
            @PathVariable String languageCode) {
        return assetProcessingApi.findByLocalization(contentId, LanguageCode.from(languageCode))
                .map(AdminAssetProcessingResponse::from)
                .orElseThrow(() -> new AssetProcessingNotFoundException(contentId, LanguageCode.from(languageCode)));
    }

    @GetMapping
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
        @NotBlank(message = "languageCode is required")
        String languageCode,
        @NotNull(message = "contentType is required")
        AssetProcessingContentType contentType,
        @NotBlank(message = "externalKey is required")
        String externalKey,
        @Positive(message = "coverSourceAssetId must be positive")
        Long coverSourceAssetId,
        @Positive(message = "audioSourceAssetId must be positive")
        Long audioSourceAssetId,
        @Min(value = 0, message = "pageCount must not be negative")
        Integer pageCount) {

    ScheduleAssetProcessingCommand toCommand() {
        return new ScheduleAssetProcessingCommand(
                contentId,
                LanguageCode.from(languageCode),
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
        Integer pageCount) {

    RetryAssetProcessingCommand toCommand(Long contentId, String languageCode) {
        return new RetryAssetProcessingCommand(
                contentId,
                LanguageCode.from(languageCode),
                contentType,
                externalKey,
                coverSourceAssetId,
                audioSourceAssetId,
                pageCount);
    }
}
