package com.tellpal.v2.presentation.api.admin;

import com.tellpal.v2.asset.application.AssetProcessingApplicationService;
import com.tellpal.v2.asset.domain.AssetProcessing;
import com.tellpal.v2.presentation.dto.asset.AssetProcessingResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/contents")
public class AssetProcessingController {

    private final AssetProcessingApplicationService assetProcessingApplicationService;

    public AssetProcessingController(AssetProcessingApplicationService assetProcessingApplicationService) {
        this.assetProcessingApplicationService = assetProcessingApplicationService;
    }

    /**
     * Manually trigger asset processing for a content localization.
     * POST /api/admin/contents/{id}/localizations/{lang}/process-assets
     */
    @PostMapping("/{id}/localizations/{lang}/process-assets")
    public ResponseEntity<AssetProcessingResponse> processAssets(
            @PathVariable Long id,
            @PathVariable String lang) {
        AssetProcessing processing = assetProcessingApplicationService.startProcessing(id, lang);
        return ResponseEntity.status(202).body(toResponse(processing));
    }

    /**
     * Query the processing status for a content localization.
     * GET /api/admin/contents/{id}/localizations/{lang}/processing-status
     */
    @GetMapping("/{id}/localizations/{lang}/processing-status")
    public ResponseEntity<AssetProcessingResponse> getProcessingStatus(
            @PathVariable Long id,
            @PathVariable String lang) {
        AssetProcessing processing = assetProcessingApplicationService.getProcessingStatus(id, lang);
        return ResponseEntity.ok(toResponse(processing));
    }

    private AssetProcessingResponse toResponse(AssetProcessing p) {
        return new AssetProcessingResponse(
                p.getContentId(),
                p.getLanguageCode(),
                p.getStatus().name(),
                p.getStartedAt(),
                p.getCompletedAt(),
                p.getErrorMessage()
        );
    }
}
