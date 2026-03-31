package com.tellpal.v2.content.web.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
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
class ContentPublicationAdminIntegrationTest extends AdminApiIntegrationTestSupport {

    private static final String SAMPLE_CHECKSUM =
            "89abcdef0123456789abcdef0123456789abcdef0123456789abcdef01234567";

    @Autowired
    private AssetRegistryApi assetRegistryApi;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("""
                truncate table
                    admin_refresh_tokens,
                    admin_user_roles,
                    admin_users,
                    content_free_access,
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
    void publishAndArchiveEndpointsRespectPublicationRules() throws Exception {
        String accessToken = authenticateAdmin();
        Long coverMediaId = registerImageAsset("/content/story/moonlight/cover.jpg");
        Long illustrationMediaId = registerImageAsset("/content/story/moonlight/page-1.jpg");
        Long audioMediaId = registerAudioAsset("/content/story/moonlight/page-1.mp3");
        Long contentId = createStoryContent(accessToken);

        createLocalization(accessToken, contentId, coverMediaId);
        addStoryPage(accessToken, contentId, illustrationMediaId);

        mockMvc.perform(put("/api/admin/contents/{contentId}/story-pages/1/localizations/tr", contentId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "bodyText": "Bir varmis bir yokmus.",
                                  "illustrationMediaId": %d
                                }
                                """.formatted(illustrationMediaId)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/contents/{contentId}/localizations/tr/publish", contentId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "publishedAt": "2026-03-17T11:00:00Z"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("content_state_conflict"))
                .andExpect(jsonPath("$.detail", containsString("audio media")));

        mockMvc.perform(put("/api/admin/contents/{contentId}/story-pages/1/localizations/tr", contentId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "bodyText": "Bir varmis bir yokmus.",
                                  "audioMediaId": %d,
                                  "illustrationMediaId": %d
                                }
                                """.formatted(audioMediaId, illustrationMediaId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.audioMediaId").value(audioMediaId))
                .andExpect(jsonPath("$.illustrationMediaId").value(illustrationMediaId));

        mockMvc.perform(post("/api/admin/contents/{contentId}/localizations/tr/publish", contentId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "publishedAt": "2026-03-17T11:00:00Z"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.publishedAt").value("2026-03-17T11:00:00Z"));

        mockMvc.perform(post("/api/admin/contents/{contentId}/localizations/tr/archive", contentId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"))
                .andExpect(jsonPath("$.publishedAt").value("2026-03-17T11:00:00Z"));

        String persistedStatus = jdbcTemplate.queryForObject(
                """
                        select status
                        from content_localizations
                        where content_id = ? and language_code = ?
                        """,
                String.class,
                contentId,
                "tr");
        assertThat(persistedStatus).isEqualTo("ARCHIVED");
    }

    private Long createStoryContent(String accessToken) throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/admin/contents")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "type": "STORY",
                                  "externalKey": "moonlight-publication",
                                  "ageRange": 5,
                                  "active": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        return readPayload(createResult).get("contentId").asLong();
    }

    private void createLocalization(String accessToken, Long contentId, Long coverMediaId) throws Exception {
        mockMvc.perform(post("/api/admin/contents/{contentId}/localizations/tr", contentId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "title": "Ay Isigi",
                                  "description": "Gece masali",
                                  "coverMediaId": %d,
                                  "status": "DRAFT",
                                  "processingStatus": "PENDING"
                                }
                                """.formatted(coverMediaId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.languageCode").value("tr"));
    }

    private void addStoryPage(String accessToken, Long contentId, Long illustrationMediaId) throws Exception {
        mockMvc.perform(post("/api/admin/contents/{contentId}/story-pages", contentId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "pageNumber": 1
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pageNumber").value(1));
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

    private Long registerAudioAsset(String objectPath) {
        return assetRegistryApi.register(new RegisterMediaAssetCommand(
                AssetStorageProvider.LOCAL_STUB,
                objectPath,
                AssetKind.ORIGINAL_AUDIO,
                "audio/mpeg",
                4096L,
                SAMPLE_CHECKSUM)).assetId();
    }
}
