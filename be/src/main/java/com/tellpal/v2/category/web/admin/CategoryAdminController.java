package com.tellpal.v2.category.web.admin;

import java.util.List;
import java.util.Optional;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.tellpal.v2.category.api.CategoryLookupApi;
import com.tellpal.v2.category.application.CategoryApplicationExceptions.CategoryNotFoundException;
import com.tellpal.v2.category.application.CategoryManagementCommands.CreateCategoryCommand;
import com.tellpal.v2.category.application.CategoryManagementCommands.CreateCategoryLocalizationCommand;
import com.tellpal.v2.category.application.CategoryManagementCommands.DeleteCategoryCommand;
import com.tellpal.v2.category.application.CategoryManagementCommands.UpdateCategoryCommand;
import com.tellpal.v2.category.application.CategoryManagementCommands.UpdateCategoryLocalizationCommand;
import com.tellpal.v2.category.application.CategoryManagementService;
import com.tellpal.v2.category.domain.CategoryType;
import com.tellpal.v2.category.domain.LocalizationStatus;
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
@RequestMapping("/api/admin/categories")
@Tag(name = "Admin Categories", description = "Category management endpoints for admin users.")
@SecurityRequirement(name = "adminBearerAuth")
public class CategoryAdminController {

    private final CategoryLookupApi categoryLookupApi;
    private final CategoryManagementService categoryManagementService;

    public CategoryAdminController(
            CategoryLookupApi categoryLookupApi,
            CategoryManagementService categoryManagementService) {
        this.categoryLookupApi = categoryLookupApi;
        this.categoryManagementService = categoryManagementService;
    }

    @GetMapping
    @Operation(summary = "List categories", description = "Returns category metadata for CMS list screens, including inactive entries.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Category list returned"),
            @ApiResponse(responseCode = "401", description = "Admin token is missing or invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "403", description = "Admin user lacks permission", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public List<AdminCategoryResponse> listCategories() {
        return categoryLookupApi.listAll().stream()
                .map(AdminCategoryResponse::from)
                .toList();
    }

    @PostMapping
    @Operation(summary = "Create a category", description = "Creates a new category aggregate with its base metadata.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Category created"),
            @ApiResponse(responseCode = "400", description = "Category request is invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "401", description = "Admin token is missing or invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "403", description = "Admin user lacks permission", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "409", description = "Category slug is already in use", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public ResponseEntity<AdminCategoryResponse> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        AdminCategoryResponse response = AdminCategoryResponse.from(
                categoryManagementService.createCategory(request.toCommand()));
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{categoryId}")
                .buildAndExpand(response.categoryId())
                .toUri())
                .body(response);
    }

    @GetMapping("/{categoryId}")
    @Operation(summary = "Get one category", description = "Returns category metadata for one category identifier.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Category returned"),
            @ApiResponse(responseCode = "401", description = "Admin token is missing or invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "403", description = "Admin user lacks permission", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "404", description = "Category was not found", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public AdminCategoryResponse getCategory(@PathVariable Long categoryId) {
        return categoryLookupApi.findById(categoryId)
                .map(AdminCategoryResponse::from)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
    }

    @GetMapping("/{categoryId}/localizations")
    @Operation(summary = "List category localizations", description = "Returns localized category snapshots for one category identifier.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Category localizations returned"),
            @ApiResponse(responseCode = "401", description = "Admin token is missing or invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "403", description = "Admin user lacks permission", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "404", description = "Category was not found", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public List<AdminCategoryLocalizationResponse> listLocalizations(@PathVariable Long categoryId) {
        return categoryLookupApi.listLocalizations(categoryId).stream()
                .map(AdminCategoryLocalizationResponse::from)
                .toList();
    }

    @DeleteMapping("/{categoryId}")
    @Operation(summary = "Delete a category", description = "Deactivates one category aggregate and preserves editorial history for admin reads.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Category deleted"),
            @ApiResponse(responseCode = "401", description = "Admin token is missing or invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "403", description = "Admin user lacks permission", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "404", description = "Category was not found", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public ResponseEntity<Void> deleteCategory(@PathVariable Long categoryId) {
        categoryManagementService.deleteCategory(new DeleteCategoryCommand(categoryId));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{categoryId}")
    @Operation(summary = "Update a category", description = "Updates the core metadata of an existing category.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Category updated"),
            @ApiResponse(responseCode = "400", description = "Category update is invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "401", description = "Admin token is missing or invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "403", description = "Admin user lacks permission", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "404", description = "Category was not found", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "409", description = "Category slug is already in use", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public AdminCategoryResponse updateCategory(
            @PathVariable Long categoryId,
            @Valid @RequestBody UpdateCategoryRequest request) {
        return AdminCategoryResponse.from(categoryManagementService.updateCategory(request.toCommand(categoryId)));
    }

    @PostMapping("/{categoryId}/localizations/{languageCode}")
    @Operation(summary = "Create a category localization", description = "Creates localized category content for one language.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Category localization created"),
            @ApiResponse(responseCode = "400", description = "Localization request is invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "401", description = "Admin token is missing or invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "403", description = "Admin user lacks permission", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "404", description = "Category was not found", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "409", description = "Category localization already exists", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public ResponseEntity<AdminCategoryLocalizationResponse> createLocalization(
            @PathVariable Long categoryId,
            @PathVariable String languageCode,
            @Valid @RequestBody UpsertCategoryLocalizationRequest request) {
        AdminCategoryLocalizationResponse response = AdminCategoryLocalizationResponse.from(
                categoryManagementService.createLocalization(request.toCreateCommand(categoryId, languageCode)));
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri())
                .body(response);
    }

    @PutMapping("/{categoryId}/localizations/{languageCode}")
    @Operation(summary = "Update a category localization", description = "Updates one localized category representation.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Category localization updated"),
            @ApiResponse(responseCode = "400", description = "Localization update is invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "401", description = "Admin token is missing or invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "403", description = "Admin user lacks permission", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "404", description = "Category or localization was not found", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public AdminCategoryLocalizationResponse updateLocalization(
            @PathVariable Long categoryId,
            @PathVariable String languageCode,
            @Valid @RequestBody UpsertCategoryLocalizationRequest request) {
        return AdminCategoryLocalizationResponse.from(
                categoryManagementService.updateLocalization(request.toUpdateCommand(categoryId, languageCode)));
    }
}

record CreateCategoryRequest(
        @NotBlank(message = "slug is required")
        String slug,
        @NotNull(message = "type is required")
        CategoryType type,
        @NotNull(message = "premium is required")
        Boolean premium,
        @NotNull(message = "active is required")
        Boolean active) {

    CreateCategoryCommand toCommand() {
        return new CreateCategoryCommand(slug, type, premium, active);
    }
}

record UpdateCategoryRequest(
        @NotBlank(message = "slug is required")
        String slug,
        @NotNull(message = "type is required")
        CategoryType type,
        @NotNull(message = "premium is required")
        Boolean premium,
        @NotNull(message = "active is required")
        Boolean active) {

    UpdateCategoryCommand toCommand(Long categoryId) {
        return new UpdateCategoryCommand(categoryId, slug, type, premium, active);
    }
}

record UpsertCategoryLocalizationRequest(
        @NotBlank(message = "name is required")
        String name,
        String description,
        @Positive(message = "imageMediaId must be positive")
        Long imageMediaId,
        @NotNull(message = "status is required")
        LocalizationStatus status,
        java.time.Instant publishedAt) {

    CreateCategoryLocalizationCommand toCreateCommand(Long categoryId, String languageCode) {
        return new CreateCategoryLocalizationCommand(
                categoryId,
                LanguageCode.from(languageCode),
                name,
                description,
                imageMediaId,
                status,
                publishedAt);
    }

    UpdateCategoryLocalizationCommand toUpdateCommand(Long categoryId, String languageCode) {
        return new UpdateCategoryLocalizationCommand(
                categoryId,
                LanguageCode.from(languageCode),
                name,
                description,
                imageMediaId,
                status,
                publishedAt);
    }
}
