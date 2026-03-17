package com.tellpal.v2.content.web.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import com.tellpal.v2.content.api.ContentReference;
import com.tellpal.v2.content.api.ResolvedContentFreeAccessSet;
import com.tellpal.v2.content.application.ContentFreeAccessService;
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
class FreeAccessAdminIntegrationTest extends AdminApiIntegrationTestSupport {

    @Autowired
    private ContentManagementService contentManagementService;

    @Autowired
    private ContentFreeAccessService contentFreeAccessService;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("""
                truncate table
                    admin_refresh_tokens,
                    admin_user_roles,
                    admin_users,
                    content_free_access,
                    story_page_localizations,
                    story_pages,
                    content_localizations,
                    contents
                restart identity cascade
                """);
    }

    @Test
    void grantListRevokeAndUnknownKeyFallbackWorkWithAuthenticatedAdmin() throws Exception {
        String accessToken = authenticateAdmin();
        Long contentId = createLocalizedStory("free-default-tr", LanguageCode.TR);

        mockMvc.perform(post("/api/admin/free-access")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "accessKey": "default",
                                  "contentId": %d,
                                  "languageCode": "tr"
                                }
                                """.formatted(contentId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessKey").value("default"))
                .andExpect(jsonPath("$.contentId").value(contentId));

        mockMvc.perform(get("/api/admin/free-access")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("accessKey", "default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].contentId").value(contentId))
                .andExpect(jsonPath("$[0].languageCode").value("tr"));

        ResolvedContentFreeAccessSet fallbackSet = contentFreeAccessService.resolveFreeAccess(
                LanguageCode.TR,
                "missing_key");
        assertThat(fallbackSet.accessKey()).isEqualTo("default");
        assertThat(fallbackSet.contentIds()).contains(contentId);

        mockMvc.perform(delete("/api/admin/free-access/default/languages/tr/contents/{contentId}", contentId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        Integer remainingRows = jdbcTemplate.queryForObject(
                "select count(*) from content_free_access where content_id = ?",
                Integer.class,
                contentId);
        assertThat(remainingRows).isZero();
    }

    @Test
    void duplicateFreeAccessReturnsConflictProblemDetails() throws Exception {
        String accessToken = authenticateAdmin();
        Long contentId = createLocalizedStory("free-duplicate-tr", LanguageCode.TR);

        mockMvc.perform(post("/api/admin/free-access")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "accessKey": "default",
                                  "contentId": %d,
                                  "languageCode": "tr"
                                }
                                """.formatted(contentId)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/admin/free-access")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "accessKey": "default",
                                  "contentId": %d,
                                  "languageCode": "tr"
                                }
                                """.formatted(contentId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Content free access already exists"))
                .andExpect(jsonPath("$.errorCode").value("content_free_access_exists"));
    }

    private Long createLocalizedStory(String externalKey, LanguageCode languageCode) {
        ContentReference content = contentManagementService.createContent(
                new CreateContentCommand(ContentType.STORY, externalKey, 4, true));
        contentManagementService.createLocalization(new CreateContentLocalizationCommand(
                content.contentId(),
                languageCode,
                "Story " + externalKey,
                "Localized description",
                null,
                null,
                null,
                null,
                LocalizationStatus.DRAFT,
                ProcessingStatus.PENDING,
                null));
        return content.contentId();
    }
}
