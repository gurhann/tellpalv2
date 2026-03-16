package com.tellpal.v2.presentation.api.public_api;

import com.tellpal.v2.category.application.CategoryApplicationService;
import com.tellpal.v2.category.application.CurationService;
import com.tellpal.v2.category.domain.Category;
import com.tellpal.v2.category.domain.CategoryContent;
import com.tellpal.v2.category.domain.CategoryLocalization;
import com.tellpal.v2.category.domain.CategoryType;
import com.tellpal.v2.content.application.ContentApplicationService;
import com.tellpal.v2.content.application.FreeAccessService;
import com.tellpal.v2.content.domain.Content;
import com.tellpal.v2.content.domain.ContentLocalization;
import com.tellpal.v2.presentation.dto.category.PublicCategoryResponse;
import com.tellpal.v2.presentation.dto.content.PublicContentResponse;
import com.tellpal.v2.shared.domain.LocalizationStatus;
import com.tellpal.v2.shared.domain.ProcessingStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/categories")
public class CategoryPublicController {

    private final CategoryApplicationService categoryApplicationService;
    private final CurationService curationService;
    private final ContentApplicationService contentApplicationService;
    private final FreeAccessService freeAccessService;

    public CategoryPublicController(CategoryApplicationService categoryApplicationService,
                                    CurationService curationService,
                                    ContentApplicationService contentApplicationService,
                                    FreeAccessService freeAccessService) {
        this.categoryApplicationService = categoryApplicationService;
        this.curationService = curationService;
        this.contentApplicationService = contentApplicationService;
        this.freeAccessService = freeAccessService;
    }

    @GetMapping
    public ResponseEntity<List<PublicCategoryResponse>> listCategories(
            @RequestParam String lang,
            @RequestParam(required = false) String type) {

        CategoryType categoryType = type != null ? CategoryType.valueOf(type) : null;

        List<PublicCategoryResponse> responses = categoryApplicationService.listCategories(categoryType)
                .stream()
                .flatMap(category -> {
                    Optional<CategoryLocalization> locOpt = categoryApplicationService
                            .findLocalization(category.getId(), lang);
                    return locOpt
                            .filter(loc -> loc.getStatus() == LocalizationStatus.PUBLISHED)
                            .map(loc -> toPublicCategoryResponse(category, loc))
                            .stream();
                })
                .toList();

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{slug}")
    public ResponseEntity<PublicCategoryResponse> getCategoryBySlug(
            @PathVariable String slug,
            @RequestParam String lang) {

        Category category = categoryApplicationService.getCategoryBySlug(slug);
        Optional<CategoryLocalization> locOpt = categoryApplicationService
                .findLocalization(category.getId(), lang);

        if (locOpt.isEmpty() || locOpt.get().getStatus() != LocalizationStatus.PUBLISHED) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(toPublicCategoryResponse(category, locOpt.get()));
    }

    @GetMapping("/{slug}/contents")
    public ResponseEntity<List<PublicContentResponse>> getCategoryContents(
            @PathVariable String slug,
            @RequestParam String lang,
            @RequestParam(required = false) String freeKey) {

        Category category = categoryApplicationService.getCategoryBySlug(slug);

        List<PublicContentResponse> responses = curationService
                .getCategoryContents(category.getId(), lang)
                .stream()
                .flatMap(cc -> {
                    Optional<ContentLocalization> locOpt = contentApplicationService
                            .findLocalization(cc.getContentId(), lang);
                    return locOpt
                            .filter(loc -> loc.getStatus() == LocalizationStatus.PUBLISHED
                                    && loc.getProcessingStatus() == ProcessingStatus.COMPLETED)
                            .map(loc -> {
                                Content content = contentApplicationService.getContent(cc.getContentId());
                                boolean isFree = freeAccessService.isFree(cc.getContentId(), lang, freeKey);
                                return new PublicContentResponse(
                                        content.getId(), content.getType().name(), content.getExternalKey(),
                                        loc.getTitle(), loc.getDescription(),
                                        isFree, content.getPageCount());
                            })
                            .stream();
                })
                .toList();

        return ResponseEntity.ok(responses);
    }

    private PublicCategoryResponse toPublicCategoryResponse(Category category, CategoryLocalization loc) {
        return new PublicCategoryResponse(
                category.getId(), category.getSlug(), category.getType().name(),
                category.isPremium(), loc.getName(), loc.getDescription());
    }
}
