package com.tellpal.v2.content.web.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
}
