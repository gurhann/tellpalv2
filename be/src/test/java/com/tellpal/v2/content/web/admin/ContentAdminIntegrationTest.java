package com.tellpal.v2.content.web.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

        mockMvc.perform(get("/api/admin/contents"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/contents/1"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/contents/1/story-pages"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/contents/1/story-pages/1"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/admin/contents/1"))
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
                                {}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pageNumber").value(1));

        mockMvc.perform(put("/api/admin/contents/{contentId}/story-pages/1/localizations/tr", contentId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "bodyText": "Bir varmis bir yokmus.",
                                  "illustrationMediaId": %d
                                }
                                """.formatted(illustrationMediaId)))
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

    @Test
    void listAndGetContentReturnLocalizationSnapshotsForAdminReadFlows() throws Exception {
        String accessToken = authenticateAdmin();

        MvcResult activeContentResult = mockMvc.perform(post("/api/admin/contents")
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

        Long activeContentId = readPayload(activeContentResult).get("contentId").asLong();

        MvcResult inactiveContentResult = mockMvc.perform(post("/api/admin/contents")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "type": "AUDIO_STORY",
                                  "externalKey": "quiet-audio",
                                  "ageRange": 7,
                                  "active": false
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        Long inactiveContentId = readPayload(inactiveContentResult).get("contentId").asLong();

        mockMvc.perform(post("/api/admin/contents/{contentId}/localizations/tr", activeContentId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "title": "Ay Isigi",
                                  "description": "Gece masali",
                                  "status": "PUBLISHED",
                                  "processingStatus": "COMPLETED",
                                  "publishedAt": "2026-03-17T09:00:00Z"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.visibleToMobile").value(true));

        mockMvc.perform(get("/api/admin/contents")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].contentId").value(activeContentId))
                .andExpect(jsonPath("$[0].localizations[0].languageCode").value("tr"))
                .andExpect(jsonPath("$[1].contentId").value(inactiveContentId))
                .andExpect(jsonPath("$[1].active").value(false))
                .andExpect(jsonPath("$[1].localizations.length()").value(0));

        mockMvc.perform(get("/api/admin/contents/{contentId}", activeContentId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contentId").value(activeContentId))
                .andExpect(jsonPath("$.externalKey").value("moonlight-story"))
                .andExpect(jsonPath("$.localizations[0].title").value("Ay Isigi"))
                .andExpect(jsonPath("$.localizations[0].processingStatus").value("COMPLETED"));
    }

    @Test
    void listAndGetStoryPagesReturnLocalizedPagePayloadsForStoryContent() throws Exception {
        String accessToken = authenticateAdmin();
        Long illustrationMediaId = registerImageAsset("/content/story/moonlight/page-1.jpg");
        Long audioMediaId = registerAudioAsset("/content/story/moonlight/page-1.mp3");

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

        mockMvc.perform(post("/api/admin/contents/{contentId}/localizations/tr", contentId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "title": "Ay Isigi",
                                  "description": "Gece masali",
                                  "status": "DRAFT",
                                  "processingStatus": "PENDING"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/admin/contents/{contentId}/story-pages", contentId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {}
                                """))
                .andExpect(status().isCreated());

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
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/contents/{contentId}/story-pages", contentId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].contentId").value(contentId))
                .andExpect(jsonPath("$[0].pageNumber").value(1))
                .andExpect(jsonPath("$[0].localizations[0].languageCode").value("tr"))
                .andExpect(jsonPath("$[0].localizations[0].audioMediaId").value(audioMediaId))
                .andExpect(jsonPath("$[0].localizations[0].illustrationMediaId").value(illustrationMediaId));

        mockMvc.perform(get("/api/admin/contents/{contentId}/story-pages/1", contentId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contentId").value(contentId))
                .andExpect(jsonPath("$.pageNumber").value(1))
                .andExpect(jsonPath("$.localizations[0].bodyText").value("Bir varmis bir yokmus."))
                .andExpect(jsonPath("$.localizations[0].illustrationMediaId").value(illustrationMediaId));
    }

    @Test
    void deleteContentDeactivatesAggregateAndPreservesAdminReadAccess() throws Exception {
        String accessToken = authenticateAdmin();

        MvcResult createResult = mockMvc.perform(post("/api/admin/contents")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "type": "STORY",
                                  "externalKey": "delete-me-story",
                                  "ageRange": 5,
                                  "active": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        Long contentId = readPayload(createResult).get("contentId").asLong();

        mockMvc.perform(delete("/api/admin/contents/{contentId}", contentId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        Boolean active = jdbcTemplate.queryForObject(
                "select is_active from contents where id = ?",
                Boolean.class,
                contentId);
        assertThat(active).isFalse();

        mockMvc.perform(get("/api/admin/contents/{contentId}", contentId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contentId").value(contentId))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void missingReadAndDeleteEndpointsReturnNotFoundForUnknownContent() throws Exception {
        String accessToken = authenticateAdmin();

        mockMvc.perform(get("/api/admin/contents/999")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("content_not_found"));

        mockMvc.perform(delete("/api/admin/contents/999")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("content_not_found"));
    }

    @Test
    void storyPageReadEndpointsReturnNotFoundAndConflictWhenParentStateIsInvalid() throws Exception {
        String accessToken = authenticateAdmin();

        mockMvc.perform(get("/api/admin/contents/999/story-pages")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("content_not_found"));

        MvcResult createResult = mockMvc.perform(post("/api/admin/contents")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "type": "MEDITATION",
                                  "externalKey": "quiet-breathing",
                                  "active": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        Long nonStoryContentId = readPayload(createResult).get("contentId").asLong();

        mockMvc.perform(get("/api/admin/contents/{contentId}/story-pages", nonStoryContentId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("content_state_conflict"));

        mockMvc.perform(get("/api/admin/contents/{contentId}/story-pages/1", nonStoryContentId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("content_state_conflict"));
    }

    @Test
    void getMissingStoryPageReturnsNotFoundProblemDetails() throws Exception {
        String accessToken = authenticateAdmin();

        MvcResult createResult = mockMvc.perform(post("/api/admin/contents")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "type": "STORY",
                                  "externalKey": "missing-story-page",
                                  "active": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        Long contentId = readPayload(createResult).get("contentId").asLong();

        mockMvc.perform(get("/api/admin/contents/{contentId}/story-pages/9", contentId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("story_page_not_found"));
    }

    @Test
    void storyPageLocalizationRequiresIllustrationMediaId() throws Exception {
        String accessToken = authenticateAdmin();

        MvcResult createResult = mockMvc.perform(post("/api/admin/contents")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "type": "STORY",
                                  "externalKey": "story-page-illustration-required",
                                  "active": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        Long contentId = readPayload(createResult).get("contentId").asLong();

        mockMvc.perform(post("/api/admin/contents/{contentId}/localizations/tr", contentId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "title": "Ay Isigi",
                                  "description": "Gece masali",
                                  "status": "DRAFT",
                                  "processingStatus": "PENDING"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/admin/contents/{contentId}/story-pages", contentId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/api/admin/contents/{contentId}/story-pages/1/localizations/tr", contentId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "bodyText": "Bir varmis bir yokmus."
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("validation_error"))
                .andExpect(jsonPath("$.fieldErrors.illustrationMediaId").value("illustrationMediaId is required"));
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
                2048L,
                SAMPLE_CHECKSUM)).assetId();
    }
}
