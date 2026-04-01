package com.tellpal.v2.content.web.admin;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.tellpal.v2.content.application.ContributorManagementCommands.AssignContentContributorCommand;
import com.tellpal.v2.content.application.ContributorManagementCommands.CreateContributorCommand;
import com.tellpal.v2.content.application.ContributorManagementCommands.RenameContributorCommand;
import com.tellpal.v2.content.application.ContributorManagementService;
import com.tellpal.v2.content.domain.ContributorRole;
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
@RequestMapping("/api/admin")
@Tag(name = "Admin Contributors", description = "Contributor management and assignment endpoints.")
@SecurityRequirement(name = "adminBearerAuth")
public class ContributorAdminController {

    private final ContributorManagementService contributorManagementService;

    public ContributorAdminController(ContributorManagementService contributorManagementService) {
        this.contributorManagementService = contributorManagementService;
    }

    @PostMapping("/contributors")
    @Operation(summary = "Create a contributor", description = "Creates a reusable contributor profile for content credits.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Contributor created"),
            @ApiResponse(responseCode = "400", description = "Contributor request is invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "401", description = "Admin token is missing or invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "403", description = "Admin user lacks permission", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public ResponseEntity<AdminContributorResponse> createContributor(
            @Valid @RequestBody CreateContributorRequest request) {
        AdminContributorResponse response = AdminContributorResponse.from(
                contributorManagementService.createContributor(request.toCommand()));
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{contributorId}")
                .buildAndExpand(response.contributorId())
                .toUri())
                .body(response);
    }

    @GetMapping("/contributors")
    @Operation(summary = "List contributors", description = "Returns recent contributor profiles for admin selection flows.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contributors returned"),
            @ApiResponse(responseCode = "400", description = "List request is invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "401", description = "Admin token is missing or invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "403", description = "Admin user lacks permission", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public List<AdminContributorResponse> listContributors(
            @RequestParam(name = "limit", defaultValue = "20") @Min(value = 1, message = "limit must be positive")
            int limit) {
        return contributorManagementService.listContributors(limit).stream()
                .map(AdminContributorResponse::from)
                .toList();
    }

    @PutMapping("/contributors/{contributorId}")
    @Operation(summary = "Rename a contributor", description = "Updates the display name of an existing contributor.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contributor renamed"),
            @ApiResponse(responseCode = "400", description = "Rename request is invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "401", description = "Admin token is missing or invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "403", description = "Admin user lacks permission", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "404", description = "Contributor was not found", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public AdminContributorResponse renameContributor(
            @PathVariable Long contributorId,
            @Valid @RequestBody RenameContributorRequest request) {
        return AdminContributorResponse.from(contributorManagementService.renameContributor(
                request.toCommand(contributorId)));
    }

    @PostMapping("/contents/{contentId}/contributors")
    @Operation(
            summary = "Assign a contributor to content",
            description = "Adds one contributor credit to content, optionally scoped to one language.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Contributor assigned to content"),
            @ApiResponse(responseCode = "400", description = "Assignment request is invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "401", description = "Admin token is missing or invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "403", description = "Admin user lacks permission", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "404", description = "Content or contributor was not found", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "409", description = "Contributor assignment conflicts with existing content state", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public ResponseEntity<AdminContentContributorResponse> assignContributor(
            @PathVariable Long contentId,
            @Valid @RequestBody AssignContentContributorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AdminContentContributorResponse.from(
                        contributorManagementService.assignContentContributor(request.toCommand(contentId))));
    }
}

record CreateContributorRequest(
        @NotBlank(message = "displayName is required")
        String displayName) {

    CreateContributorCommand toCommand() {
        return new CreateContributorCommand(displayName);
    }
}

record RenameContributorRequest(
        @NotBlank(message = "displayName is required")
        String displayName) {

    RenameContributorCommand toCommand(Long contributorId) {
        return new RenameContributorCommand(contributorId, displayName);
    }
}

record AssignContentContributorRequest(
        @NotNull(message = "contributorId is required")
        @Positive(message = "contributorId must be positive")
        Long contributorId,
        @NotNull(message = "role is required")
        ContributorRole role,
        String languageCode,
        String creditName,
        @Min(value = 0, message = "sortOrder must not be negative")
        int sortOrder) {

    AssignContentContributorCommand toCommand(Long contentId) {
        LanguageCode resolvedLanguageCode = null;
        if (languageCode != null) {
            String normalizedLanguageCode = languageCode.trim();
            if (normalizedLanguageCode.isEmpty()) {
                throw new IllegalArgumentException("languageCode must not be blank when provided");
            }
            resolvedLanguageCode = LanguageCode.from(normalizedLanguageCode);
        }
        return new AssignContentContributorCommand(
                contentId,
                contributorId,
                role,
                resolvedLanguageCode,
                creditName,
                sortOrder);
    }
}
