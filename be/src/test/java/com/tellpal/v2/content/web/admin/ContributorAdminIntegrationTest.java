package com.tellpal.v2.content.web.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.tellpal.v2.content.api.ContentReference;
import com.tellpal.v2.content.application.ContentManagementCommands.CreateContentCommand;
import com.tellpal.v2.content.application.ContentManagementService;
import com.tellpal.v2.content.domain.ContentType;
import com.tellpal.v2.support.AdminApiIntegrationTestSupport;

@SpringBootTest
@AutoConfigureMockMvc
class ContributorAdminIntegrationTest extends AdminApiIntegrationTestSupport {

    @Autowired
    private ContentManagementService contentManagementService;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("""
                truncate table
                    admin_refresh_tokens,
                    admin_user_roles,
                    admin_users,
                    content_contributors,
                    contributors,
                    content_localizations,
                    story_page_localizations,
                    story_pages,
                    contents,
                    media_assets
                restart identity cascade
                """);
    }

    @Test
    void contributorCreateListRenameAndAssignWorkWithAuthenticatedAdmin() throws Exception {
        String accessToken = authenticateAdmin();
        ContentReference content = contentManagementService.createContent(
                new CreateContentCommand(ContentType.STORY, "forest-story", 5, true));

        MvcResult createResult = mockMvc.perform(post("/api/admin/contributors")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "displayName": "Elif Yilmaz"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        Long contributorId = readPayload(createResult).get("contributorId").asLong();

        mockMvc.perform(get("/api/admin/contributors")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].contributorId").value(contributorId));

        mockMvc.perform(put("/api/admin/contributors/{contributorId}", contributorId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "displayName": "Elif Kaya"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Elif Kaya"));

        mockMvc.perform(post("/api/admin/contents/{contentId}/contributors", content.contentId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "contributorId": %d,
                                  "role": "AUTHOR",
                                  "languageCode": "tr",
                                  "creditName": "E. Kaya",
                                  "sortOrder": 0
                                }
                                """.formatted(contributorId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contributorId").value(contributorId))
                .andExpect(jsonPath("$.role").value("AUTHOR"));
    }

    @Test
    void contributorValidationReturnsProblemDetails() throws Exception {
        String accessToken = authenticateAdmin();

        mockMvc.perform(post("/api/admin/contributors")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "displayName": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("validation_error"))
                .andExpect(jsonPath("$.fieldErrors.displayName").value("displayName is required"));
    }

    @Test
    void globalContributorAssignmentWorksWithoutContentLocalizations() throws Exception {
        String accessToken = authenticateAdmin();
        ContentReference content = contentManagementService.createContent(
                new CreateContentCommand(ContentType.STORY, "global-credit-story", 4, true));
        Long contributorId = createContributor(accessToken, "Global Author");

        mockMvc.perform(post("/api/admin/contents/{contentId}/contributors", content.contentId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "contributorId": %d,
                                  "role": "AUTHOR",
                                  "creditName": "Global Author",
                                  "sortOrder": 0
                                }
                                """.formatted(contributorId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contributorId").value(contributorId))
                .andExpect(jsonPath("$.role").value("AUTHOR"))
                .andExpect(jsonPath("$.languageCode").value(Matchers.nullValue()));
    }

    @Test
    void sameContributorCanHaveGlobalAndLocalizedCreditsForTheSameRole() throws Exception {
        String accessToken = authenticateAdmin();
        ContentReference content = contentManagementService.createContent(
                new CreateContentCommand(ContentType.STORY, "mixed-credit-story", 4, true));
        Long contributorId = createContributor(accessToken, "Mixed Scope Author");

        mockMvc.perform(post("/api/admin/contents/{contentId}/contributors", content.contentId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "contributorId": %d,
                                  "role": "AUTHOR",
                                  "creditName": "Global Author",
                                  "sortOrder": 0
                                }
                                """.formatted(contributorId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.languageCode").value(Matchers.nullValue()));

        mockMvc.perform(post("/api/admin/contents/{contentId}/contributors", content.contentId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "contributorId": %d,
                                  "role": "AUTHOR",
                                  "languageCode": "en",
                                  "creditName": "English Author",
                                  "sortOrder": 0
                                }
                                """.formatted(contributorId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.languageCode").value("en"));
    }

    @Test
    void blankLanguageCodeIsRejectedWhenGlobalScopeIsRequestedIncorrectly() throws Exception {
        String accessToken = authenticateAdmin();
        ContentReference content = contentManagementService.createContent(
                new CreateContentCommand(ContentType.STORY, "blank-language-story", 4, true));
        Long contributorId = createContributor(accessToken, "Blank Language Author");

        mockMvc.perform(post("/api/admin/contents/{contentId}/contributors", content.contentId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "contributorId": %d,
                                  "role": "AUTHOR",
                                  "languageCode": "   ",
                                  "creditName": null,
                                  "sortOrder": 0
                                }
                                """.formatted(contributorId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("invalid_request"))
                .andExpect(jsonPath("$.detail").value("languageCode must not be blank when provided"));
    }

    @Test
    void duplicateGlobalScopeAssignmentAndSortOrderStillFail() throws Exception {
        String accessToken = authenticateAdmin();
        ContentReference content = contentManagementService.createContent(
                new CreateContentCommand(ContentType.STORY, "duplicate-global-story", 4, true));
        Long authorId = createContributor(accessToken, "Duplicate Global Author");
        Long illustratorId = createContributor(accessToken, "Duplicate Global Illustrator");

        mockMvc.perform(post("/api/admin/contents/{contentId}/contributors", content.contentId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "contributorId": %d,
                                  "role": "AUTHOR",
                                  "creditName": null,
                                  "sortOrder": 0
                                }
                                """.formatted(authorId)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/admin/contents/{contentId}/contributors", content.contentId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "contributorId": %d,
                                  "role": "AUTHOR",
                                  "creditName": null,
                                  "sortOrder": 0
                                }
                                """.formatted(authorId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("invalid_request"));

        mockMvc.perform(post("/api/admin/contents/{contentId}/contributors", content.contentId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "contributorId": %d,
                                  "role": "AUTHOR",
                                  "creditName": null,
                                  "sortOrder": 0
                                }
                                """.formatted(illustratorId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("invalid_request"));
    }

    private Long createContributor(String accessToken, String displayName) throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/admin/contributors")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "displayName": "%s"
                                }
                                """.formatted(displayName)))
                .andExpect(status().isCreated())
                .andReturn();

        return readPayload(createResult).get("contributorId").asLong();
    }
}
