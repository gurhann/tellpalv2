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

@AdminApiController
@RequestMapping("/api/admin")
public class ContributorAdminController {

    private final ContributorManagementService contributorManagementService;

    public ContributorAdminController(ContributorManagementService contributorManagementService) {
        this.contributorManagementService = contributorManagementService;
    }

    @PostMapping("/contributors")
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
    public List<AdminContributorResponse> listContributors(
            @RequestParam(name = "limit", defaultValue = "20") @Min(value = 1, message = "limit must be positive")
            int limit) {
        return contributorManagementService.listContributors(limit).stream()
                .map(AdminContributorResponse::from)
                .toList();
    }

    @PutMapping("/contributors/{contributorId}")
    public AdminContributorResponse renameContributor(
            @PathVariable Long contributorId,
            @Valid @RequestBody RenameContributorRequest request) {
        return AdminContributorResponse.from(contributorManagementService.renameContributor(
                request.toCommand(contributorId)));
    }

    @PostMapping("/contents/{contentId}/contributors")
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
        @NotBlank(message = "languageCode is required")
        String languageCode,
        String creditName,
        @Min(value = 0, message = "sortOrder must not be negative")
        int sortOrder) {

    AssignContentContributorCommand toCommand(Long contentId) {
        return new AssignContentContributorCommand(
                contentId,
                contributorId,
                role,
                LanguageCode.from(languageCode),
                creditName,
                sortOrder);
    }
}
