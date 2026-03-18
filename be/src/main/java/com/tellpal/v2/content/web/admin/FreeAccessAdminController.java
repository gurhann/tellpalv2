package com.tellpal.v2.content.web.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.tellpal.v2.content.application.ContentFreeAccessCommands.GrantContentFreeAccessCommand;
import com.tellpal.v2.content.application.ContentFreeAccessCommands.RevokeContentFreeAccessCommand;
import com.tellpal.v2.content.application.ContentFreeAccessService;
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
@RequestMapping("/api/admin/free-access")
@Tag(name = "Admin Free Access", description = "Free-access grant management endpoints.")
@SecurityRequirement(name = "adminBearerAuth")
public class FreeAccessAdminController {

    private final ContentFreeAccessService contentFreeAccessService;

    public FreeAccessAdminController(ContentFreeAccessService contentFreeAccessService) {
        this.contentFreeAccessService = contentFreeAccessService;
    }

    @PostMapping
    @Operation(summary = "Grant free access", description = "Creates a free-access grant for one localized content item.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Free-access grant created"),
            @ApiResponse(responseCode = "400", description = "Grant request is invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "401", description = "Admin token is missing or invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "403", description = "Admin user lacks permission", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "404", description = "Content or localization was not found", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "409", description = "Free-access grant already exists", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public ResponseEntity<AdminContentFreeAccessResponse> grantFreeAccess(
            @Valid @RequestBody CreateContentFreeAccessRequest request) {
        AdminContentFreeAccessResponse response = AdminContentFreeAccessResponse.from(
                contentFreeAccessService.grantFreeAccess(request.toCommand()));
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{accessKey}/languages/{languageCode}/contents/{contentId}")
                .buildAndExpand(response.accessKey(), response.languageCode(), response.contentId())
                .toUri())
                .body(response);
    }

    @GetMapping
    @Operation(summary = "List free-access grants", description = "Returns all free-access grants for one access key or the effective default set.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Free-access grants returned"),
            @ApiResponse(responseCode = "401", description = "Admin token is missing or invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "403", description = "Admin user lacks permission", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public java.util.List<AdminContentFreeAccessResponse> listFreeAccessEntries(
            @RequestParam(name = "accessKey", required = false) String accessKey) {
        return contentFreeAccessService.listFreeAccessEntries(accessKey).stream()
                .map(AdminContentFreeAccessResponse::from)
                .toList();
    }

    @DeleteMapping("/{accessKey}/languages/{languageCode}/contents/{contentId}")
    @Operation(summary = "Revoke free access", description = "Removes one free-access grant for a localized content item.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Free-access grant removed"),
            @ApiResponse(responseCode = "401", description = "Admin token is missing or invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "403", description = "Admin user lacks permission", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "404", description = "Free-access grant was not found", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public ResponseEntity<Void> revokeFreeAccess(
            @PathVariable String accessKey,
            @PathVariable String languageCode,
            @PathVariable Long contentId) {
        contentFreeAccessService.revokeFreeAccess(new RevokeContentFreeAccessCommand(
                accessKey,
                contentId,
                LanguageCode.from(languageCode)));
        return ResponseEntity.noContent().build();
    }
}

record CreateContentFreeAccessRequest(
        @NotBlank(message = "accessKey is required")
        String accessKey,
        @Positive(message = "contentId must be positive")
        Long contentId,
        @NotBlank(message = "languageCode is required")
        String languageCode) {

    GrantContentFreeAccessCommand toCommand() {
        return new GrantContentFreeAccessCommand(accessKey, contentId, LanguageCode.from(languageCode));
    }
}
