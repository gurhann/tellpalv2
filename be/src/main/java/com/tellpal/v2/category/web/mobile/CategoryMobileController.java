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

@RestController
@RequestMapping("/api/categories")
public class CategoryMobileController {

    private final PublicCategoryQueryApi publicCategoryQueryApi;

    public CategoryMobileController(PublicCategoryQueryApi publicCategoryQueryApi) {
        this.publicCategoryQueryApi = publicCategoryQueryApi;
    }

    @GetMapping
    public List<MobileCategoryResponse> listCategories(
            @RequestParam("lang") String languageCode,
            @RequestParam(name = "type", required = false) CategoryApiType type) {
        return publicCategoryQueryApi.listCategories(LanguageCode.from(languageCode), type).stream()
                .map(MobileCategoryResponse::from)
                .toList();
    }

    @GetMapping("/{slug}")
    public MobileCategoryResponse getCategory(
            @PathVariable String slug,
            @RequestParam("lang") String languageCode) {
        return publicCategoryQueryApi.findCategory(slug, LanguageCode.from(languageCode))
                .map(MobileCategoryResponse::from)
                .orElseThrow(() -> new MobileCategoryNotFoundException(slug, languageCode));
    }

    @GetMapping("/{slug}/contents")
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
