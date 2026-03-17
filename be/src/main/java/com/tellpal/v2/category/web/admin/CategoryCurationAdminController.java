package com.tellpal.v2.category.web.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.tellpal.v2.category.application.CategoryCurationService;
import com.tellpal.v2.category.application.CategoryManagementCommands.AddCategoryContentCommand;
import com.tellpal.v2.category.application.CategoryManagementCommands.RemoveCategoryContentCommand;
import com.tellpal.v2.category.application.CategoryManagementCommands.UpdateCategoryContentOrderCommand;
import com.tellpal.v2.shared.domain.LanguageCode;
import com.tellpal.v2.shared.web.admin.AdminApiController;

@AdminApiController
@RequestMapping("/api/admin/categories/{categoryId}/localizations/{languageCode}/contents")
public class CategoryCurationAdminController {

    private final CategoryCurationService categoryCurationService;

    public CategoryCurationAdminController(CategoryCurationService categoryCurationService) {
        this.categoryCurationService = categoryCurationService;
    }

    @PostMapping
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
