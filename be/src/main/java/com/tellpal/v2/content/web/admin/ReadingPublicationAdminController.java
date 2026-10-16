package com.tellpal.v2.content.web.admin;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ProblemDetail;
import com.tellpal.v2.content.api.ReadingPublicationApi;
import com.tellpal.v2.shared.web.admin.AdminApiController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Exact-language feedback for the internal local/test contract; no delivery, access or mobile rollout. */
@AdminApiController
@RequestMapping("/api/admin/contents/{contentId}/reading-publication")
@Tag(name = "Admin Reading Publication", description = "Internal preparation feedback and explicit retry.")
@SecurityRequirement(name = "adminBearerAuth")
@ApiResponses( {
    @ApiResponse(responseCode = "400", description = "Invalid exact language", content = @Content(schema = @Schema(implementation = ProblemDetail.class))), @ApiResponse(responseCode = "401", description = "Admin token missing or invalid", content = @Content(schema = @Schema(implementation = ProblemDetail.class))), @ApiResponse(responseCode = "403", description = "Admin permission required", content = @Content(schema = @Schema(implementation = ProblemDetail.class))), @ApiResponse(responseCode = "404", description = "Content not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
}) public class ReadingPublicationAdminController {
    private final ReadingPublicationApi publication;
    public ReadingPublicationAdminController(ReadingPublicationApi publication) {
        this.publication = publication;
    }

    @GetMapping
    @Operation(summary = "Get internal reading publication status", description = "Disabled means legacy behavior. Failure diagnostics are finite codes without storage paths or exception details; LIVE certifies only this local/test contract.")
    @ApiResponse(responseCode = "200", description = "Capability and exact-language status")
    public ReadingPublicationApi.Status status(@PathVariable long contentId, @RequestParam String languageCode) {
        return publication.status(contentId, languageCode);
    }

    @PostMapping("/retry")
    @Operation(summary = "Retry current failed reading publication", description = "Idempotently requests current failed obligations; no automatic mutation retry or additional revision approval.")
    @ApiResponse(responseCode = "200", description = "Current capability and status after retry request")
    public ReadingPublicationApi.Status retry(@PathVariable long contentId, @RequestParam String languageCode) {
        return publication.retry(contentId, languageCode);
    }
}
