package com.tellpal.v2.content.web.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.tellpal.v2.content.api.AdminStoryPageQueryApi;
import com.tellpal.v2.content.application.ContentApplicationExceptions.StoryPageNotFoundException;
import com.tellpal.v2.content.application.ContentManagementCommands.AddStoryPageCommand;
import com.tellpal.v2.content.application.ContentManagementCommands.RemoveStoryPageCommand;
import com.tellpal.v2.content.application.ContentManagementCommands.UpdateStoryPageCommand;
import com.tellpal.v2.content.application.ContentManagementCommands.UpsertStoryPageLocalizationCommand;
import com.tellpal.v2.content.application.StoryPageManagementService;
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
@RequestMapping("/api/admin/contents/{contentId}/story-pages")
@Tag(name = "Admin Story Pages", description = "Story page management endpoints for story content.")
@SecurityRequirement(name = "adminBearerAuth")
public class StoryPageAdminController {

    private final AdminStoryPageQueryApi adminStoryPageQueryApi;
    private final StoryPageManagementService storyPageManagementService;

    public StoryPageAdminController(
            AdminStoryPageQueryApi adminStoryPageQueryApi,
            StoryPageManagementService storyPageManagementService) {
        this.adminStoryPageQueryApi = adminStoryPageQueryApi;
        this.storyPageManagementService = storyPageManagementService;
    }

    @GetMapping
    @Operation(
            summary = "List story pages",
            description = "Returns the story-page collection and localized page payloads for one story content item.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Story-page list returned"),
            @ApiResponse(responseCode = "401", description = "Admin token is missing or invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "403", description = "Admin user lacks permission", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "404", description = "Content was not found", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "409", description = "Story pages are unavailable for the current content state", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public List<AdminStoryPageReadResponse> listStoryPages(@PathVariable Long contentId) {
        return adminStoryPageQueryApi.listStoryPages(contentId).stream()
                .map(AdminStoryPageReadResponse::from)
                .toList();
    }

    @GetMapping("/{pageNumber}")
    @Operation(
            summary = "Get one story page",
            description = "Returns one story page and its localized payloads for story-page editor detail screens.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Story page returned"),
            @ApiResponse(responseCode = "401", description = "Admin token is missing or invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "403", description = "Admin user lacks permission", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "404", description = "Content or story page was not found", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "409", description = "Story pages are unavailable for the current content state", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public AdminStoryPageReadResponse getStoryPage(@PathVariable Long contentId, @PathVariable int pageNumber) {
        return adminStoryPageQueryApi.findStoryPage(contentId, pageNumber)
                .map(AdminStoryPageReadResponse::from)
                .orElseThrow(() -> new StoryPageNotFoundException(contentId, pageNumber));
    }

    @PostMapping
    @Operation(summary = "Add a story page", description = "Creates one story page under a story content item.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Story page created"),
            @ApiResponse(responseCode = "400", description = "Story page request is invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "401", description = "Admin token is missing or invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "403", description = "Admin user lacks permission", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "404", description = "Content was not found", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "409", description = "Story page conflicts with current content state", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public ResponseEntity<AdminStoryPageResponse> addStoryPage(
            @PathVariable Long contentId,
            @Valid @RequestBody AddStoryPageRequest request) {
        AdminStoryPageResponse response = AdminStoryPageResponse.from(
                storyPageManagementService.addStoryPage(request.toCommand(contentId)));
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{pageNumber}")
                .buildAndExpand(response.pageNumber())
                .toUri())
                .body(response);
    }

    @PutMapping("/{pageNumber}")
    @Operation(summary = "Update a story page", description = "Refreshes one story page snapshot after page-level edits.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Story page updated"),
            @ApiResponse(responseCode = "400", description = "Story page update is invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "401", description = "Admin token is missing or invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "403", description = "Admin user lacks permission", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "404", description = "Story page was not found", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public AdminStoryPageResponse updateStoryPage(
            @PathVariable Long contentId,
            @PathVariable int pageNumber,
            @Valid @RequestBody UpdateStoryPageRequest request) {
        return AdminStoryPageResponse.from(
                storyPageManagementService.updateStoryPage(request.toCommand(contentId, pageNumber)));
    }

    @DeleteMapping("/{pageNumber}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a story page", description = "Deletes one story page from a story content item.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Story page removed"),
            @ApiResponse(responseCode = "401", description = "Admin token is missing or invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "403", description = "Admin user lacks permission", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "404", description = "Story page was not found", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public void removeStoryPage(@PathVariable Long contentId, @PathVariable int pageNumber) {
        storyPageManagementService.removeStoryPage(new RemoveStoryPageCommand(contentId, pageNumber));
    }

    @PutMapping("/{pageNumber}/localizations/{languageCode}")
    @Operation(summary = "Upsert story page localization", description = "Creates or updates one localized story page payload.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Story page localization upserted"),
            @ApiResponse(responseCode = "400", description = "Localization request is invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "401", description = "Admin token is missing or invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "403", description = "Admin user lacks permission", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "404", description = "Story page or content was not found", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public AdminStoryPageLocalizationResponse upsertStoryPageLocalization(
            @PathVariable Long contentId,
            @PathVariable int pageNumber,
            @PathVariable String languageCode,
            @Valid @RequestBody UpsertStoryPageLocalizationRequest request) {
        return AdminStoryPageLocalizationResponse.from(
                storyPageManagementService.upsertStoryPageLocalization(
                        request.toCommand(contentId, pageNumber, languageCode)));
    }
}

record AddStoryPageRequest(
        @Positive(message = "afterPageNumber must be positive")
        Integer afterPageNumber) {

    AddStoryPageCommand toCommand(Long contentId) {
        return new AddStoryPageCommand(contentId, afterPageNumber);
    }
}

record UpdateStoryPageRequest() {

    UpdateStoryPageCommand toCommand(Long contentId, int pageNumber) {
        return new UpdateStoryPageCommand(contentId, pageNumber);
    }
}

record UpsertStoryPageLocalizationRequest(
        String bodyText,
        @Positive(message = "audioMediaId must be positive")
        Long audioMediaId,
        @NotNull(message = "illustrationMediaId is required")
        @Positive(message = "illustrationMediaId must be positive")
        Long illustrationMediaId) {

    UpsertStoryPageLocalizationCommand toCommand(Long contentId, int pageNumber, String languageCode) {
        return new UpsertStoryPageLocalizationCommand(
                contentId,
                pageNumber,
                LanguageCode.from(languageCode),
                bodyText,
                audioMediaId,
                illustrationMediaId);
    }
}
