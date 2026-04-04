package com.tellpal.v2.category.web.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.tellpal.v2.category.api.AdminCategoryContentView;
import com.tellpal.v2.category.api.AdminCategoryCurationQueryApi;
import com.tellpal.v2.category.api.CategoryLookupApi;
import com.tellpal.v2.category.api.CategoryReference;
import com.tellpal.v2.category.application.CategoryApplicationExceptions.CategoryLocalizationNotFoundException;
import com.tellpal.v2.category.application.CategoryApplicationExceptions.CategoryContentTypeMismatchException;
import com.tellpal.v2.category.application.CategoryApplicationExceptions.CategoryLocalizationNotPublishedException;
import com.tellpal.v2.category.application.CategoryApplicationExceptions.CategoryNotFoundException;
import com.tellpal.v2.category.application.CategoryManagementResults.CategoryContentRecord;
import com.tellpal.v2.category.application.CategoryManagementResults.CategoryLocalizationRecord;
import com.tellpal.v2.category.application.CategoryCurationService;
import com.tellpal.v2.category.application.CategoryManagementService;
import com.tellpal.v2.category.domain.CategoryType;
import com.tellpal.v2.category.domain.LocalizationStatus;
import com.tellpal.v2.content.api.ContentApiType;
import com.tellpal.v2.shared.domain.LanguageCode;
import com.tellpal.v2.shared.web.admin.AdminApiExceptionHandler;
import com.tellpal.v2.shared.web.admin.AdminAuthenticationFacade;
import com.tellpal.v2.shared.web.admin.AdminProblemDetailsFactory;

@WebMvcTest({CategoryAdminController.class, CategoryCurationAdminController.class})
@AutoConfigureMockMvc(addFilters = false)
@Import({CategoryAdminExceptionHandler.class, AdminApiExceptionHandler.class, AdminProblemDetailsFactory.class})
class CategoryAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryLookupApi categoryLookupApi;

    @MockitoBean
    private CategoryManagementService categoryManagementService;

    @MockitoBean
    private AdminCategoryCurationQueryApi categoryCurationQueryApi;

    @MockitoBean
    private CategoryCurationService categoryCurationService;

    @MockitoBean
    private AdminAuthenticationFacade adminAuthenticationFacade;

    @Test
    void createCategoryReturnsCreatedResponse() throws Exception {
        when(categoryManagementService.createCategory(any())).thenReturn(new CategoryReference(
                42L,
                ContentApiType.STORY,
                "featured-sleep",
                false,
                true));

        mockMvc.perform(post("/api/admin/categories")
                        .contentType("application/json")
                        .content("""
                                {
                                  "slug": "featured-sleep",
                                  "type": "STORY",
                                  "premium": false,
                                  "active": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/admin/categories/42"))
                .andExpect(jsonPath("$.categoryId").value(42))
                .andExpect(jsonPath("$.type").value("STORY"))
                .andExpect(jsonPath("$.slug").value("featured-sleep"));
    }

    @Test
    void listCategoriesReturnsLookupResponses() throws Exception {
        when(categoryLookupApi.listAll()).thenReturn(List.of(
                new CategoryReference(42L, ContentApiType.STORY, "featured-sleep", false, true),
                new CategoryReference(43L, ContentApiType.MEDITATION, "sleep-routines", true, false)));

        mockMvc.perform(get("/api/admin/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].categoryId").value(42))
                .andExpect(jsonPath("$[0].slug").value("featured-sleep"))
                .andExpect(jsonPath("$[0].active").value(true))
                .andExpect(jsonPath("$[1].categoryId").value(43))
                .andExpect(jsonPath("$[1].type").value("MEDITATION"))
                .andExpect(jsonPath("$[1].active").value(false));
    }

    @Test
    void getCategoryReturnsLookupResponse() throws Exception {
        when(categoryLookupApi.findById(42L)).thenReturn(Optional.of(new CategoryReference(
                42L,
                ContentApiType.LULLABY,
                "sleep-routines",
                true,
                true)));

        mockMvc.perform(get("/api/admin/categories/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").value(42))
                .andExpect(jsonPath("$.type").value("LULLABY"))
                .andExpect(jsonPath("$.premium").value(true));
    }

    @Test
    void missingCategoryReturnsNotFoundProblemDetails() throws Exception {
        when(categoryLookupApi.findById(42L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/admin/categories/42"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Category not found"))
                .andExpect(jsonPath("$.errorCode").value("category_not_found"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void deleteCategoryReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/admin/categories/42"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteMissingCategoryReturnsNotFoundProblemDetails() throws Exception {
        doThrow(new CategoryNotFoundException(42L))
                .when(categoryManagementService)
                .deleteCategory(any());

        mockMvc.perform(delete("/api/admin/categories/42"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Category not found"))
                .andExpect(jsonPath("$.errorCode").value("category_not_found"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void createLocalizationReturnsCreatedResponse() throws Exception {
        when(categoryManagementService.createLocalization(any())).thenReturn(new CategoryLocalizationRecord(
                42L,
                LanguageCode.TR,
                "Uyku",
                "Gece rutini",
                15L,
                LocalizationStatus.PUBLISHED,
                Instant.parse("2026-03-17T09:00:00Z"),
                true));

        mockMvc.perform(post("/api/admin/categories/42/localizations/tr")
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "Uyku",
                                  "description": "Gece rutini",
                                  "imageMediaId": 15,
                                  "status": "PUBLISHED",
                                  "publishedAt": "2026-03-17T09:00:00Z"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/admin/categories/42/localizations/tr"))
                .andExpect(jsonPath("$.languageCode").value("tr"))
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.published").value(true));
    }

    @Test
    void addCuratedContentReturnsCreatedResponse() throws Exception {
        when(categoryCurationService.addContent(any())).thenReturn(new CategoryContentRecord(
                42L,
                LanguageCode.TR,
                77L,
                0));

        mockMvc.perform(post("/api/admin/categories/42/localizations/tr/contents")
                        .contentType("application/json")
                        .content("""
                                {
                                  "contentId": 77,
                                  "displayOrder": 0
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "http://localhost/api/admin/categories/42/localizations/tr/contents/77"))
                .andExpect(jsonPath("$.contentId").value(77))
                .andExpect(jsonPath("$.displayOrder").value(0));
    }

    @Test
    void listCuratedContentReturnsOrderedResponses() throws Exception {
        when(categoryCurationQueryApi.listCategoryContents(42L, LanguageCode.TR)).thenReturn(List.of(
                new AdminCategoryContentView(42L, LanguageCode.TR, 77L, 0),
                new AdminCategoryContentView(42L, LanguageCode.TR, 91L, 3)));

        mockMvc.perform(get("/api/admin/categories/42/localizations/tr/contents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].categoryId").value(42))
                .andExpect(jsonPath("$[0].languageCode").value("tr"))
                .andExpect(jsonPath("$[0].contentId").value(77))
                .andExpect(jsonPath("$[0].displayOrder").value(0))
                .andExpect(jsonPath("$[1].contentId").value(91))
                .andExpect(jsonPath("$[1].displayOrder").value(3));
    }

    @Test
    void updateCuratedContentOrderReturnsUpdatedResponse() throws Exception {
        when(categoryCurationService.updateContentOrder(any())).thenReturn(new CategoryContentRecord(
                42L,
                LanguageCode.EN,
                91L,
                3));

        mockMvc.perform(put("/api/admin/categories/42/localizations/en/contents/91")
                        .contentType("application/json")
                        .content("""
                                {
                                  "displayOrder": 3
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.languageCode").value("en"))
                .andExpect(jsonPath("$.contentId").value(91))
                .andExpect(jsonPath("$.displayOrder").value(3));
    }

    @Test
    void unpublishedCategoryLocalizationBecomesConflictProblemDetails() throws Exception {
        when(categoryCurationService.addContent(any()))
                .thenThrow(new CategoryLocalizationNotPublishedException(42L, LanguageCode.TR));

        mockMvc.perform(post("/api/admin/categories/42/localizations/tr/contents")
                        .contentType("application/json")
                        .content("""
                                {
                                  "contentId": 77,
                                  "displayOrder": 0
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Category localization not published"))
                .andExpect(jsonPath("$.errorCode").value("category_localization_not_published"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void missingCategoryLocalizationInCurationReadReturnsNotFoundProblemDetails() throws Exception {
        when(categoryCurationQueryApi.listCategoryContents(42L, LanguageCode.TR))
                .thenThrow(new CategoryLocalizationNotFoundException(42L, LanguageCode.TR));

        mockMvc.perform(get("/api/admin/categories/42/localizations/tr/contents"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Category localization not found"))
                .andExpect(jsonPath("$.errorCode").value("category_localization_not_found"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void contentTypeMismatchBecomesConflictProblemDetails() throws Exception {
        when(categoryCurationService.addContent(any()))
                .thenThrow(new CategoryContentTypeMismatchException(
                        77L,
                        CategoryType.MEDITATION,
                        ContentApiType.STORY));

        mockMvc.perform(post("/api/admin/categories/42/localizations/tr/contents")
                        .contentType("application/json")
                        .content("""
                                {
                                  "contentId": 77,
                                  "displayOrder": 0
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Category content type mismatch"))
                .andExpect(jsonPath("$.errorCode").value("category_content_type_mismatch"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }
}
