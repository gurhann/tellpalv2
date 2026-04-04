package com.tellpal.v2.category.web.admin;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

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

import com.tellpal.v2.category.api.AdminCategoryCurationQueryApi;
import com.tellpal.v2.category.application.CategoryCurationService;
import com.tellpal.v2.category.application.CategoryManagementCommands.AddCategoryContentCommand;
import com.tellpal.v2.category.application.CategoryManagementCommands.RemoveCategoryContentCommand;
import com.tellpal.v2.category.application.CategoryManagementCommands.UpdateCategoryContentOrderCommand;
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
@RequestMapping("/api/admin/categories/{categoryId}/localizations/{languageCode}/contents")
@Tag(name = "Admin Category Curation", description = "Curated content assignment endpoints for category localizations.")
@SecurityRequirement(name = "adminBearerAuth")
public class CategoryCurationAdminController {

    private final AdminCategoryCurationQueryApi categoryCurationQueryApi;
    private final CategoryCurationService categoryCurationService;

    public CategoryCurationAdminController(
            AdminCategoryCurationQueryApi categoryCurationQueryApi,
            CategoryCurationService categoryCurationService) {
        this.categoryCurationQueryApi = categoryCurationQueryApi;
        this.categoryCurationService = categoryCurationService;
    }

    @GetMapping
    @Operation(
            summary = "List curated content",
            description = "Returns the ordered curated content links stored for one category localization.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Localized curated content collection returned"),
            @ApiResponse(responseCode = "401", description = "Admin token is missing or invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "403", description = "Admin user lacks permission", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "404", description = "Category or category localization was not found", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public List<AdminCategoryContentResponse> listCuratedContent(
            @PathVariable Long categoryId,
            @PathVariable String languageCode) {
        return categoryCurationQueryApi.listCategoryContents(categoryId, LanguageCode.from(languageCode)).stream()
                .map(AdminCategoryContentResponse::from)
                .toList();
    }

    @PostMapping
    @Operation(summary = "Add curated content", description = "Adds one content item to a localized category curation list.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Curated content added"),
            @ApiResponse(responseCode = "400", description = "Add request is invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "401", description = "Admin token is missing or invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "403", description = "Admin user lacks permission", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "404", description = "Category localization or content was not found", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "409", description = "Category curation state does not allow the change", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public ResponseEntity<AdminCategoryContentResponse> addCuratedContent(
            @PathVariable Long categoryId,
            @PathVariable String languageCode,
            @Valid @RequestBody AddCategoryContentRequest request) {
        AdminCategoryContentResponse response = AdminCategoryContentResponse.from(
                categoryCurationService.addContent(request.toCommand(categoryId, languageCode)));
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{contentId}")
                .buildAndExpand(response.contentId())
                .toUri())
                .body(response);
    }

    @PutMapping("/{contentId}")
    @Operation(summary = "Update curated content order", description = "Changes the display order for one curated content item.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Curated content order updated"),
            @ApiResponse(responseCode = "400", description = "Update request is invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "401", description = "Admin token is missing or invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "403", description = "Admin user lacks permission", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "404", description = "Curated content entry was not found", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public AdminCategoryContentResponse updateCuratedContentOrder(
            @PathVariable Long categoryId,
            @PathVariable String languageCode,
            @PathVariable Long contentId,
            @Valid @RequestBody UpdateCategoryContentOrderRequest request) {
        return AdminCategoryContentResponse.from(categoryCurationService.updateContentOrder(
                request.toCommand(categoryId, languageCode, contentId)));
    }

    @DeleteMapping("/{contentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove curated content", description = "Removes one content item from a localized category curation list.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Curated content removed"),
            @ApiResponse(responseCode = "401", description = "Admin token is missing or invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "403", description = "Admin user lacks permission", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "404", description = "Curated content entry was not found", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public void removeCuratedContent(
            @PathVariable Long categoryId,
            @PathVariable String languageCode,
            @PathVariable Long contentId) {
        categoryCurationService.removeContent(new RemoveCategoryContentCommand(
                categoryId,
                LanguageCode.from(languageCode),
                contentId));
    }
}

record AddCategoryContentRequest(
        @NotNull(message = "contentId is required")
        @Positive(message = "contentId must be positive")
        Long contentId,
        @Min(value = 0, message = "displayOrder must not be negative")
        int displayOrder) {

    AddCategoryContentCommand toCommand(Long categoryId, String languageCode) {
        return new AddCategoryContentCommand(categoryId, LanguageCode.from(languageCode), contentId, displayOrder);
    }
}

record UpdateCategoryContentOrderRequest(
        @Min(value = 0, message = "displayOrder must not be negative")
        int displayOrder) {

    UpdateCategoryContentOrderCommand toCommand(Long categoryId, String languageCode, Long contentId) {
        return new UpdateCategoryContentOrderCommand(
                categoryId,
                LanguageCode.from(languageCode),
                contentId,
                displayOrder);
    }
}
