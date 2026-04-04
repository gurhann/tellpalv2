package com.tellpal.v2.category.web.admin;

import static org.hamcrest.Matchers.hasSize;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.tellpal.v2.content.api.ContentReference;
import com.tellpal.v2.content.application.ContentManagementCommands.CreateContentCommand;
import com.tellpal.v2.content.application.ContentManagementCommands.CreateContentLocalizationCommand;
import com.tellpal.v2.content.application.ContentManagementService;
import com.tellpal.v2.content.domain.ContentType;
import com.tellpal.v2.content.domain.LocalizationStatus;
import com.tellpal.v2.content.domain.ProcessingStatus;
import com.tellpal.v2.shared.domain.LanguageCode;
import com.tellpal.v2.support.AdminApiIntegrationTestSupport;

@SpringBootTest
@AutoConfigureMockMvc
class CategoryAdminIntegrationTest extends AdminApiIntegrationTestSupport {

    @Autowired
    private ContentManagementService contentManagementService;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("""
                truncate table
                    admin_refresh_tokens,
                    admin_user_roles,
                    admin_users,
                    category_contents,
                    category_localizations,
                    categories,
                    content_contributors,
                    contributors,
                    story_page_localizations,
                    story_pages,
                    content_localizations,
                    contents,
                    media_assets
                restart identity cascade
                """);
    }

    @Test
    void protectedCategoryEndpointsRequireAuthentication() throws Exception {
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
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/categories"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/categories/1"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/categories/1/localizations"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/categories/1/localizations/tr/contents"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/admin/categories/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void categoryListReturnsActiveAndInactiveRecordsForAuthenticatedAdmin() throws Exception {
        String accessToken = authenticateAdmin();

        mockMvc.perform(post("/api/admin/categories")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "slug": "featured-sleep",
                                  "type": "STORY",
                                  "premium": false,
                                  "active": true
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/admin/categories")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "slug": "sleep-routines",
                                  "type": "MEDITATION",
                                  "premium": true,
                                  "active": false
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/admin/categories")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].slug").value("featured-sleep"))
                .andExpect(jsonPath("$[0].active").value(true))
                .andExpect(jsonPath("$[1].slug").value("sleep-routines"))
                .andExpect(jsonPath("$[1].active").value(false))
                .andExpect(jsonPath("$[1].premium").value(true));
    }

    @Test
    void deleteCategoryDeactivatesAggregateAndPreservesAdminReadAccess() throws Exception {
        String accessToken = authenticateAdmin();

        MvcResult createResult = mockMvc.perform(post("/api/admin/categories")
                        .header("Authorization", "Bearer " + accessToken)
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
                .andReturn();

        Long categoryId = readPayload(createResult).get("categoryId").asLong();

        mockMvc.perform(post("/api/admin/categories/{categoryId}/localizations/tr", categoryId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "One Cikan Uyku",
                                  "description": "Editor secimleri",
                                  "status": "PUBLISHED",
                                  "publishedAt": "2026-03-17T09:00:00Z"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/admin/categories/{categoryId}", categoryId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        Boolean active = jdbcTemplate.queryForObject(
                "select is_active from categories where id = ?",
                Boolean.class,
                categoryId);
        assertThat(active).isFalse();

        Integer localizationCount = jdbcTemplate.queryForObject(
                "select count(*) from category_localizations where category_id = ?",
                Integer.class,
                categoryId);
        assertThat(localizationCount).isEqualTo(1);

        mockMvc.perform(get("/api/admin/categories/{categoryId}", categoryId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").value(categoryId))
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/admin/categories")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].categoryId").value(categoryId))
                .andExpect(jsonPath("$[0].active").value(false));
    }

    @Test
    void categoryLocalizationListReturnsPersistedSnapshotsForAuthenticatedAdmin() throws Exception {
        String accessToken = authenticateAdmin();

        MvcResult createResult = mockMvc.perform(post("/api/admin/categories")
                        .header("Authorization", "Bearer " + accessToken)
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
                .andReturn();

        Long categoryId = readPayload(createResult).get("categoryId").asLong();

        mockMvc.perform(post("/api/admin/categories/{categoryId}/localizations/tr", categoryId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "One Cikan Uyku",
                                  "description": "Editor secimleri",
                                  "status": "PUBLISHED",
                                  "publishedAt": "2026-03-17T09:00:00Z"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/admin/categories/{categoryId}/localizations", categoryId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].categoryId").value(categoryId))
                .andExpect(jsonPath("$[0].languageCode").value("tr"))
                .andExpect(jsonPath("$[0].name").value("One Cikan Uyku"))
                .andExpect(jsonPath("$[0].published").value(true));
    }

    @Test
    void categoryLocalizationListReturnsEmptyArrayWhenCategoryHasNoLocalizations() throws Exception {
        String accessToken = authenticateAdmin();

        MvcResult createResult = mockMvc.perform(post("/api/admin/categories")
                        .header("Authorization", "Bearer " + accessToken)
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
                .andReturn();

        Long categoryId = readPayload(createResult).get("categoryId").asLong();

        mockMvc.perform(get("/api/admin/categories/{categoryId}/localizations", categoryId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void missingReadAndDeleteEndpointsReturnNotFoundForUnknownCategory() throws Exception {
        String accessToken = authenticateAdmin();

        mockMvc.perform(get("/api/admin/categories/999")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("category_not_found"));

        mockMvc.perform(get("/api/admin/categories/999/localizations")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("category_not_found"));

        mockMvc.perform(delete("/api/admin/categories/999")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("category_not_found"));
    }

    @Test
    void categoryCrudLocalizationAndOrderingWorkWithAuthenticatedAdmin() throws Exception {
        String accessToken = authenticateAdmin();
        Long curatedContentId = createPublishedStoryContent("featured-night-tr", LanguageCode.TR);
        Long secondCuratedContentId = createPublishedStoryContent("moonlight-tr", LanguageCode.TR);

        MvcResult createResult = mockMvc.perform(post("/api/admin/categories")
                        .header("Authorization", "Bearer " + accessToken)
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
                .andReturn();

        Long categoryId = readPayload(createResult).get("categoryId").asLong();

        mockMvc.perform(put("/api/admin/categories/{categoryId}", categoryId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "slug": "featured-sleep-updated",
                                  "type": "STORY",
                                  "premium": true,
                                  "active": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.premium").value(true));

        mockMvc.perform(post("/api/admin/categories/{categoryId}/localizations/tr", categoryId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "One Cikan Uyku",
                                  "description": "Editor secimleri",
                                  "status": "PUBLISHED",
                                  "publishedAt": "2026-03-17T09:00:00Z"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.languageCode").value("tr"));

        mockMvc.perform(post("/api/admin/categories/{categoryId}/localizations/tr/contents", categoryId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "contentId": %d,
                                  "displayOrder": 0
                                }
                                """.formatted(curatedContentId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contentId").value(curatedContentId));

        mockMvc.perform(post("/api/admin/categories/{categoryId}/localizations/tr/contents", categoryId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "contentId": %d,
                                  "displayOrder": 5
                                }
                                """.formatted(secondCuratedContentId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contentId").value(secondCuratedContentId));

        mockMvc.perform(put("/api/admin/categories/{categoryId}/localizations/tr/contents/{contentId}",
                        categoryId,
                        curatedContentId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "displayOrder": 3
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayOrder").value(3));

        mockMvc.perform(get("/api/admin/categories/{categoryId}/localizations/tr/contents", categoryId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].contentId").value(curatedContentId))
                .andExpect(jsonPath("$[0].displayOrder").value(3))
                .andExpect(jsonPath("$[1].contentId").value(secondCuratedContentId))
                .andExpect(jsonPath("$[1].displayOrder").value(5));
    }

    @Test
    void curationRejectsUnpublishedCategoryLocalization() throws Exception {
        String accessToken = authenticateAdmin();
        Long curatedContentId = createPublishedStoryContent("featured-night-tr", LanguageCode.TR);

        MvcResult createResult = mockMvc.perform(post("/api/admin/categories")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "slug": "draft-category",
                                  "type": "STORY",
                                  "premium": false,
                                  "active": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        Long categoryId = readPayload(createResult).get("categoryId").asLong();

        mockMvc.perform(post("/api/admin/categories/{categoryId}/localizations/tr", categoryId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "Taslak",
                                  "status": "DRAFT"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/admin/categories/{categoryId}/localizations/tr/contents", categoryId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "contentId": %d,
                                  "displayOrder": 0
                                }
                                """.formatted(curatedContentId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("category_localization_not_published"));
    }

    @Test
    void curationRejectsContentTypeMismatch() throws Exception {
        String accessToken = authenticateAdmin();
        Long curatedContentId = createPublishedStoryContent("featured-night-tr", LanguageCode.TR);

        MvcResult createResult = mockMvc.perform(post("/api/admin/categories")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "slug": "meditation-only",
                                  "type": "MEDITATION",
                                  "premium": false,
                                  "active": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        Long categoryId = readPayload(createResult).get("categoryId").asLong();

        mockMvc.perform(post("/api/admin/categories/{categoryId}/localizations/tr", categoryId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "Meditasyon",
                                  "status": "PUBLISHED",
                                  "publishedAt": "2026-03-17T09:00:00Z"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/admin/categories/{categoryId}/localizations/tr/contents", categoryId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "contentId": %d,
                                  "displayOrder": 0
                                }
                                """.formatted(curatedContentId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("category_content_type_mismatch"));
    }

    @Test
    void curationReadRejectsMissingCategoryLocalization() throws Exception {
        String accessToken = authenticateAdmin();

        MvcResult createResult = mockMvc.perform(post("/api/admin/categories")
                        .header("Authorization", "Bearer " + accessToken)
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
                .andReturn();

        Long categoryId = readPayload(createResult).get("categoryId").asLong();

        mockMvc.perform(get("/api/admin/categories/{categoryId}/localizations/tr/contents", categoryId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("category_localization_not_found"));
    }

    private Long createPublishedStoryContent(String externalKey, LanguageCode languageCode) {
        ContentReference content = contentManagementService.createContent(
                new CreateContentCommand(ContentType.STORY, externalKey, 4, true));
        contentManagementService.createLocalization(new CreateContentLocalizationCommand(
                content.contentId(),
                languageCode,
                "Story " + externalKey,
                null,
                null,
                null,
                null,
                null,
                LocalizationStatus.PUBLISHED,
                ProcessingStatus.PENDING,
                Instant.parse("2026-03-17T09:00:00Z")));
        return content.contentId();
    }
}
