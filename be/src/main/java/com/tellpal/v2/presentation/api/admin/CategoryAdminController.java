package com.tellpal.v2.presentation.api.admin;

import com.tellpal.v2.category.application.CategoryApplicationService;
import com.tellpal.v2.category.application.CurationService;
import com.tellpal.v2.category.domain.Category;
import com.tellpal.v2.category.domain.CategoryContent;
import com.tellpal.v2.category.domain.CategoryLocalization;
import com.tellpal.v2.category.domain.CategoryType;
import com.tellpal.v2.presentation.dto.category.AddCategoryContentRequest;
import com.tellpal.v2.presentation.dto.category.CategoryContentResponse;
import com.tellpal.v2.presentation.dto.category.CategoryLocalizationResponse;
import com.tellpal.v2.presentation.dto.category.CategoryResponse;
import com.tellpal.v2.presentation.dto.category.CreateCategoryLocalizationRequest;
import com.tellpal.v2.presentation.dto.category.CreateCategoryRequest;
import com.tellpal.v2.presentation.dto.category.UpdateCategoryLocalizationRequest;
import com.tellpal.v2.presentation.dto.category.UpdateCategoryRequest;
import com.tellpal.v2.presentation.dto.category.UpdateContentOrderRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/categories")
public class CategoryAdminController {

    private final CategoryApplicationService categoryApplicationService;
    private final CurationService curationService;

    public CategoryAdminController(CategoryApplicationService categoryApplicationService,
                                   CurationService curationService) {
        this.categoryApplicationService = categoryApplicationService;
        this.curationService = curationService;
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> listCategories(@RequestParam(required = false) String type) {
        List<CategoryResponse> responses = categoryApplicationService
                .listCategories(type != null ? CategoryType.valueOf(type) : null)
                .stream()
                .map(this::toCategoryResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@RequestBody CreateCategoryRequest request) {
        Category category = categoryApplicationService.createCategory(
                request.slug(), CategoryType.valueOf(request.type()), request.isPremium());
        return ResponseEntity.status(201).body(toCategoryResponse(category));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getCategory(@PathVariable Long id) {
        Category category = categoryApplicationService.getCategory(id);
        return ResponseEntity.ok(toCategoryResponse(category));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(@PathVariable Long id,
                                                           @RequestBody UpdateCategoryRequest request) {
        Category category = categoryApplicationService.updateCategory(id, request.isActive(), request.isPremium());
        return ResponseEntity.ok(toCategoryResponse(category));
    }

    @PostMapping("/{id}/localizations")
    public ResponseEntity<CategoryLocalizationResponse> createLocalization(
            @PathVariable Long id,
            @RequestBody CreateCategoryLocalizationRequest request) {
        CategoryLocalization localization = categoryApplicationService.createLocalization(
                id, request.languageCode(), request.name(), request.description());
        return ResponseEntity.status(201).body(toLocalizationResponse(localization));
    }

    @PutMapping("/{id}/localizations/{lang}")
    public ResponseEntity<CategoryLocalizationResponse> updateLocalization(
            @PathVariable Long id,
            @PathVariable String lang,
            @RequestBody UpdateCategoryLocalizationRequest request) {
        CategoryLocalization localization = categoryApplicationService.updateLocalization(
                id, lang, request.name(), request.description());
        return ResponseEntity.ok(toLocalizationResponse(localization));
    }

    @PostMapping("/{id}/localizations/{lang}/publish")
    public ResponseEntity<Void> publishLocalization(@PathVariable Long id, @PathVariable String lang) {
        categoryApplicationService.publishLocalization(id, lang);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/contents")
    public ResponseEntity<CategoryContentResponse> addContent(@PathVariable Long id,
                                                              @RequestBody AddCategoryContentRequest request) {
        CategoryContent content = curationService.addContent(
                id, request.languageCode(), request.contentId(), request.displayOrder());
        return ResponseEntity.status(201).body(toCategoryContentResponse(content));
    }

    @PutMapping("/{id}/contents/{contentId}")
    public ResponseEntity<CategoryContentResponse> updateContentOrder(
            @PathVariable Long id,
            @PathVariable Long contentId,
            @RequestParam String lang,
            @RequestBody UpdateContentOrderRequest request) {
        CategoryContent content = curationService.updateContentOrder(id, lang, contentId, request.displayOrder());
        return ResponseEntity.ok(toCategoryContentResponse(content));
    }

    @DeleteMapping("/{id}/contents/{contentId}")
    public ResponseEntity<Void> removeContent(@PathVariable Long id,
                                              @PathVariable Long contentId,
                                              @RequestParam String lang) {
        curationService.removeContent(id, lang, contentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/contents")
    public ResponseEntity<List<CategoryContentResponse>> getCategoryContents(@PathVariable Long id,
                                                                             @RequestParam String lang) {
        List<CategoryContentResponse> responses = curationService.getCategoryContents(id, lang)
                .stream()
                .map(this::toCategoryContentResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    private CategoryResponse toCategoryResponse(Category c) {
        return new CategoryResponse(c.getId(), c.getSlug(), c.getType().name(), c.isPremium(), c.isActive());
    }

    private CategoryLocalizationResponse toLocalizationResponse(CategoryLocalization l) {
        return new CategoryLocalizationResponse(
                l.getLanguageCode(), l.getName(), l.getDescription(), l.getStatus().name());
    }

    private CategoryContentResponse toCategoryContentResponse(CategoryContent cc) {
        return new CategoryContentResponse(
                cc.getId(), cc.getCategoryId(), cc.getLanguageCode(), cc.getContentId(), cc.getDisplayOrder());
    }
}
