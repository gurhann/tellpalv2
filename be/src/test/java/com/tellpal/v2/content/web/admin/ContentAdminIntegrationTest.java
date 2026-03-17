package com.tellpal.v2.content.web.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

import com.tellpal.v2.asset.api.AssetKind;
import com.tellpal.v2.asset.api.AssetRegistryApi;
import com.tellpal.v2.asset.api.AssetStorageProvider;
import com.tellpal.v2.asset.api.RegisterMediaAssetCommand;
import com.tellpal.v2.support.AdminApiIntegrationTestSupport;

@SpringBootTest
@AutoConfigureMockMvc
class ContentAdminIntegrationTest extends AdminApiIntegrationTestSupport {

    private static final String SAMPLE_CHECKSUM =
            "9abcdef0123456789abcdef0123456789abcdef0123456789abcdef012345678";

    @Autowired
    private AssetRegistryApi assetRegistryApi;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("""
                truncate table
                    admin_refresh_tokens,
                    admin_user_roles,
                    admin_users,
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
    void protectedContentEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(post("/api/admin/contents")
                        .contentType("application/json")
                        .content("""
                                {
                                  "type": "STORY",
                                  "externalKey": "moonlight-story",
                                  "active": true
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createUpdateLocalizationProcessingAndStoryPagesWorkWithAuthenticatedAdmin() throws Exception {
        String accessToken = authenticateAdmin();
        Long coverMediaId = registerImageAsset("/content/story/moonlight/cover.jpg");
        Long illustrationMediaId = registerImageAsset("/content/story/moonlight/page-1.jpg");

        MvcResult createResult = mockMvc.perform(post("/api/admin/contents")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "type": "STORY",
                                  "externalKey": "moonlight-story",
                                  "ageRange": 5,
                                  "active": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        Long contentId = readPayload(createResult).get("contentId").asLong();

        mockMvc.perform(put("/api/admin/contents/{contentId}", contentId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "externalKey": "moonlight-story-updated",
                                  "ageRange": 6,
                                  "active": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.externalKey").value("moonlight-story-updated"));

        mockMvc.perform(post("/api/admin/contents/{contentId}/localizations/tr", contentId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "title": "Ay Isigi",
                                  "description": "Gece masali",
                                  "coverMediaId": %d,
                                  "status": "PUBLISHED",
                                  "processingStatus": "PENDING",
                                  "publishedAt": "2026-03-17T09:00:00Z"
                                }
                                """.formatted(coverMediaId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.languageCode").value("tr"));

        mockMvc.perform(patch("/api/admin/contents/{contentId}/localizations/tr/processing-status", contentId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "processingStatus": "COMPLETED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processingStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.visibleToMobile").value(true));

        mockMvc.perform(post("/api/admin/contents/{contentId}/story-pages", contentId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "pageNumber": 1,
                                  "illustrationMediaId": %d
                                }
                                """.formatted(illustrationMediaId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pageNumber").value(1));

        mockMvc.perform(put("/api/admin/contents/{contentId}/story-pages/1/localizations/tr", contentId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "bodyText": "Bir varmis bir yokmus."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.languageCode").value("tr"));

        Integer pageCount = jdbcTemplate.queryForObject(
                "select page_count from contents where id = ?",
                Integer.class,
                contentId);
        assertThat(pageCount).isEqualTo(1);
    }

    @Test
    void duplicateExternalKeyReturnsConflictProblemDetails() throws Exception {
        String accessToken = authenticateAdmin();

        mockMvc.perform(post("/api/admin/contents")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "type": "STORY",
                                  "externalKey": "duplicate-key",
                                  "active": true
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/admin/contents")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "type": "AUDIO_STORY",
                                  "externalKey": "duplicate-key",
                                  "active": true
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("duplicate_external_key"));
    }

    private Long registerImageAsset(String objectPath) {
        return assetRegistryApi.register(new RegisterMediaAssetCommand(
                AssetStorageProvider.LOCAL_STUB,
                objectPath,
                AssetKind.ORIGINAL_IMAGE,
                "image/jpeg",
                1024L,
                SAMPLE_CHECKSUM)).assetId();
    }
}
