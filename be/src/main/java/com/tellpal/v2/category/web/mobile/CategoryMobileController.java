package com.tellpal.v2.category.web.mobile;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tellpal.v2.category.api.CategoryApiType;
import com.tellpal.v2.category.api.PublicCategoryQueryApi;
import com.tellpal.v2.shared.domain.LanguageCode;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/categories")
@Tag(name = "Mobile Categories", description = "Public category discovery endpoints for mobile clients.")
public class CategoryMobileController {

    private final PublicCategoryQueryApi publicCategoryQueryApi;

    public CategoryMobileController(PublicCategoryQueryApi publicCategoryQueryApi) {
        this.publicCategoryQueryApi = publicCategoryQueryApi;
    }

    @GetMapping
    @Operation(summary = "List visible categories", description = "Returns visible categories for one language and optional type filter.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categories returned"),
            @ApiResponse(responseCode = "400", description = "Category query is invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public List<MobileCategoryResponse> listCategories(
            @RequestParam("lang") String languageCode,
            @RequestParam(name = "type", required = false) CategoryApiType type) {
        return publicCategoryQueryApi.listCategories(LanguageCode.from(languageCode), type).stream()
                .map(MobileCategoryResponse::from)
                .toList();
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get one category", description = "Returns one visible category by slug for the requested language.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Category returned"),
            @ApiResponse(responseCode = "400", description = "Category query is invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "404", description = "Category was not found", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public MobileCategoryResponse getCategory(
            @PathVariable String slug,
            @RequestParam("lang") String languageCode) {
        return publicCategoryQueryApi.findCategory(slug, LanguageCode.from(languageCode))
                .map(MobileCategoryResponse::from)
                .orElseThrow(() -> new MobileCategoryNotFoundException(slug, languageCode));
    }

    @GetMapping("/{slug}/contents")
    @Operation(summary = "List category contents", description = "Returns visible content summaries curated under one category localization.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Category contents returned"),
            @ApiResponse(responseCode = "400", description = "Category content query is invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "404", description = "Category was not found", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public List<MobileCategoryContentResponse> listCategoryContents(
            @PathVariable String slug,
            @RequestParam("lang") String languageCode,
            @RequestParam(name = "freeKey", required = false) String freeKey) {
        LanguageCode requiredLanguageCode = LanguageCode.from(languageCode);
        if (publicCategoryQueryApi.findCategory(slug, requiredLanguageCode).isEmpty()) {
            throw new MobileCategoryNotFoundException(slug, languageCode);
        }
        return publicCategoryQueryApi.listCategoryContents(slug, requiredLanguageCode, freeKey).stream()
                .map(MobileCategoryContentResponse::from)
                .toList();
    }
}
