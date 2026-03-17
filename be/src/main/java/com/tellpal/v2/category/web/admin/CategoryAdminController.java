package com.tellpal.v2.category.web.admin;

import java.util.Optional;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import org.springframework.http.ResponseEntity;
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
import com.tellpal.v2.category.application.CategoryManagementCommands.UpdateCategoryCommand;
import com.tellpal.v2.category.application.CategoryManagementCommands.UpdateCategoryLocalizationCommand;
import com.tellpal.v2.category.application.CategoryManagementService;
import com.tellpal.v2.category.domain.CategoryType;
import com.tellpal.v2.category.domain.LocalizationStatus;
import com.tellpal.v2.shared.domain.LanguageCode;
import com.tellpal.v2.shared.web.admin.AdminApiController;

@AdminApiController
@RequestMapping("/api/admin/categories")
public class CategoryAdminController {

    private final CategoryLookupApi categoryLookupApi;
    private final CategoryManagementService categoryManagementService;

    public CategoryAdminController(
            CategoryLookupApi categoryLookupApi,
            CategoryManagementService categoryManagementService) {
        this.categoryLookupApi = categoryLookupApi;
        this.categoryManagementService = categoryManagementService;
    }

    @PostMapping
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
    public AdminCategoryResponse getCategory(@PathVariable Long categoryId) {
        return categoryLookupApi.findById(categoryId)
                .map(AdminCategoryResponse::from)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
    }

    @PutMapping("/{categoryId}")
    public AdminCategoryResponse updateCategory(
            @PathVariable Long categoryId,
            @Valid @RequestBody UpdateCategoryRequest request) {
        return AdminCategoryResponse.from(categoryManagementService.updateCategory(request.toCommand(categoryId)));
    }

    @PostMapping("/{categoryId}/localizations/{languageCode}")
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
