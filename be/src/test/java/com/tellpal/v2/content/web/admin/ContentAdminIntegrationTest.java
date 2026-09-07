package com.tellpal.v2.content.web.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
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
        Long listeningCoverMediaId = registerImageAsset("/content/story/moonlight/listening-cover.jpg");
        Long narrationAudioMediaId = registerAudioAsset("/content/story/moonlight/narration.mp3");
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

        mockMvc.perform(put("/api/admin/contents/{contentId}", contentId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "externalKey": "moonlight-story-updated",
                                  "ageRange": 6,
                                  "active": true,
                                  "textlessCoverMediaId": %d,
                                  "listeningCoverMediaId": %d
                                }
                                """.formatted(coverMediaId, listeningCoverMediaId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.textlessCoverMediaId").value(coverMediaId))
                .andExpect(jsonPath("$.listeningCoverMediaId").value(listeningCoverMediaId));

        mockMvc.perform(post("/api/admin/contents/{contentId}/localizations/tr", contentId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "title": "Ay Isigi",
                                  "description": "Gece masali",
                                  "coverMediaId": %d,
                                  "narration": {
                                    "audioMediaId": %d,
                                    "durationMinutes": 11
                                  },
                                  "status": "PUBLISHED",
                                  "processingStatus": "PENDING",
                                  "publishedAt": "2026-03-17T09:00:00Z"
                                }
                                """.formatted(coverMediaId, narrationAudioMediaId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.languageCode").value("tr"))
                .andExpect(jsonPath("$.narration.audioMediaId").value(narrationAudioMediaId))
                .andExpect(jsonPath("$.narration.durationMinutes").value(11))
                .andExpect(jsonPath("$.narration.processingStatus").value("PENDING"))
                .andExpect(jsonPath("$.narration.processingError").value(nullValue()));

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
                .andExpect(jsonPath("$.visibleToMobile").value(true))
                .andExpect(jsonPath("$.narration.processingStatus").value("PENDING"));

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
    void contentLevelListeningCoverIsSharedAcrossLocalizationsAndTypeScoped() throws Exception {
        String accessToken = authenticateAdmin();
        Long sourceCoverMediaId = registerImageAsset("/content/story/shared/source-cover.jpg");
        Long firstListeningCoverMediaId = registerImageAsset("/content/story/shared/listening-cover-1.jpg");
        Long secondListeningCoverMediaId = registerImageAsset("/content/story/shared/listening-cover-2.jpg");
        Long trLocalizationCoverMediaId = registerImageAsset("/content/story/shared/tr-localization-cover.jpg");
        Long enLocalizationCoverMediaId = registerImageAsset("/content/story/shared/en-localization-cover.jpg");
        Long audioMediaId = registerAudioAsset("/content/story/shared/not-an-image.mp3");

        MvcResult createResult = mockMvc.perform(post("/api/admin/contents")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "type": "STORY",
                                  "externalKey": "shared-cover-story",
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
                                  "title": "Paylasilan kapak",
                                  "coverMediaId": %d,
                                  "status": "DRAFT",
                                  "processingStatus": "PENDING"
                                }
                                """.formatted(trLocalizationCoverMediaId)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/admin/contents/{contentId}/localizations/en", contentId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "title": "Shared cover",
                                  "coverMediaId": %d,
                                  "status": "DRAFT",
                                  "processingStatus": "PENDING"
                                }
                                """.formatted(enLocalizationCoverMediaId)))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/api/admin/contents/{contentId}", contentId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "externalKey": "shared-cover-story",
                                  "active": true,
                                  "textlessCoverMediaId": %d,
                                  "listeningCoverMediaId": %d
                                }
                                """.formatted(sourceCoverMediaId, firstListeningCoverMediaId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.textlessCoverMediaId").value(sourceCoverMediaId))
                .andExpect(jsonPath("$.listeningCoverMediaId").value(firstListeningCoverMediaId));

        mockMvc.perform(put("/api/admin/contents/{contentId}", contentId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "externalKey": "shared-cover-story",
                                  "active": true,
                                  "textlessCoverMediaId": %d,
                                  "listeningCoverMediaId": %d
                                }
                                """.formatted(sourceCoverMediaId, sourceCoverMediaId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("invalid_request"))
                .andExpect(jsonPath("$.detail")
                        .value("textlessCoverMediaId and listeningCoverMediaId must reference different assets"));

        mockMvc.perform(get("/api/admin/contents/{contentId}", contentId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.textlessCoverMediaId").value(sourceCoverMediaId))
                .andExpect(jsonPath("$.listeningCoverMediaId").value(firstListeningCoverMediaId))
                .andExpect(jsonPath("$.localizations[0].coverMediaId").value(enLocalizationCoverMediaId))
                .andExpect(jsonPath("$.localizations[1].coverMediaId").value(trLocalizationCoverMediaId));

        mockMvc.perform(get("/api/admin/contents/{contentId}", contentId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.listeningCoverMediaId").value(firstListeningCoverMediaId))
                .andExpect(jsonPath("$.localizations[0].coverMediaId").value(enLocalizationCoverMediaId))
                .andExpect(jsonPath("$.localizations[1].coverMediaId").value(trLocalizationCoverMediaId))
                .andExpect(jsonPath("$.localizations.length()").value(2));

        mockMvc.perform(put("/api/admin/contents/{contentId}", contentId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "externalKey": "shared-cover-story",
                                  "active": true,
                                  "textlessCoverMediaId": %d,
                                  "listeningCoverMediaId": %d
                                }
                                """.formatted(sourceCoverMediaId, secondListeningCoverMediaId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.textlessCoverMediaId").value(sourceCoverMediaId))
                .andExpect(jsonPath("$.listeningCoverMediaId").value(secondListeningCoverMediaId));

        mockMvc.perform(put("/api/admin/contents/{contentId}", contentId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "externalKey": "shared-cover-story",
                                  "active": true,
                                  "textlessCoverMediaId": %d,
                                  "listeningCoverMediaId": null
                                }
                                """.formatted(sourceCoverMediaId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.listeningCoverMediaId").value(nullValue()));

        mockMvc.perform(get("/api/admin/contents/{contentId}", contentId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.textlessCoverMediaId").value(sourceCoverMediaId))
                .andExpect(jsonPath("$.listeningCoverMediaId").value(nullValue()))
                .andExpect(jsonPath("$.localizations[0].coverMediaId").value(enLocalizationCoverMediaId))
                .andExpect(jsonPath("$.localizations[1].coverMediaId").value(trLocalizationCoverMediaId));

        mockMvc.perform(put("/api/admin/contents/{contentId}", contentId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "externalKey": "shared-cover-story",
                                  "active": true,
                                  "textlessCoverMediaId": %d,
                                  "listeningCoverMediaId": %d
                                }
                                """.formatted(sourceCoverMediaId, audioMediaId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("asset_media_type_mismatch"));

        mockMvc.perform(put("/api/admin/contents/{contentId}", contentId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "externalKey": "shared-cover-story",
                                  "active": true,
                                  "textlessCoverMediaId": %d,
                                  "listeningCoverMediaId": 999999
                                }
                                """.formatted(sourceCoverMediaId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("asset_not_found"));

        assertThat(jdbcTemplate.queryForObject(
                "select textless_cover_media_id from contents where id = ?", Long.class, contentId))
                .isEqualTo(sourceCoverMediaId);
        assertThat(jdbcTemplate.queryForObject(
                "select listening_cover_media_id from contents where id = ?", Long.class, contentId))
                .isNull();
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from asset_processing where content_id = ?", Long.class, contentId))
                .isZero();

        MvcResult meditationResult = mockMvc.perform(post("/api/admin/contents")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "type": "MEDITATION",
                                  "externalKey": "shared-cover-meditation",
                                  "active": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        Long meditationId = readPayload(meditationResult).get("contentId").asLong();

        mockMvc.perform(put("/api/admin/contents/{contentId}", meditationId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "externalKey": "shared-cover-meditation",
                                  "active": true,
                                  "listeningCoverMediaId": %d
                                }
                                """.formatted(firstListeningCoverMediaId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.textlessCoverMediaId").value(nullValue()))
                .andExpect(jsonPath("$.listeningCoverMediaId").value(firstListeningCoverMediaId));

        mockMvc.perform(put("/api/admin/contents/{contentId}", meditationId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "externalKey": "shared-cover-meditation",
                                  "active": true,
                                  "textlessCoverMediaId": %d,
                                  "listeningCoverMediaId": null
                                }
                                """.formatted(sourceCoverMediaId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("invalid_request"));
    }

    @Test
    void listeningCoverOwnershipIsEnforcedForLullabyAndAudioStory() throws Exception {
        String accessToken = authenticateAdmin();
        Long listeningCoverMediaId = registerImageAsset("/content/shared/listening-cover.jpg");

        MvcResult lullabyResult = mockMvc.perform(post("/api/admin/contents")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "type": "LULLABY",
                                  "externalKey": "shared-cover-lullaby",
                                  "active": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        Long lullabyId = readPayload(lullabyResult).get("contentId").asLong();

        MvcResult audioStoryResult = mockMvc.perform(post("/api/admin/contents")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "type": "AUDIO_STORY",
                                  "externalKey": "shared-cover-audio-story",
                                  "active": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        Long audioStoryId = readPayload(audioStoryResult).get("contentId").asLong();

        mockMvc.perform(put("/api/admin/contents/{contentId}", lullabyId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "externalKey": "shared-cover-lullaby",
                                  "active": true,
                                  "listeningCoverMediaId": %d
                                }
                                """.formatted(listeningCoverMediaId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.listeningCoverMediaId").value(listeningCoverMediaId));

        mockMvc.perform(put("/api/admin/contents/{contentId}", audioStoryId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "externalKey": "shared-cover-audio-story",
                                  "active": true,
                                  "listeningCoverMediaId": %d
                                }
                                """.formatted(listeningCoverMediaId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("invalid_request"));

        assertThat(jdbcTemplate.queryForObject(
                "select listening_cover_media_id from contents where id = ?", Long.class, lullabyId))
                .isEqualTo(listeningCoverMediaId);
        assertThat(jdbcTemplate.queryForObject(
                "select listening_cover_media_id from contents where id = ?", Long.class, audioStoryId))
                .isNull();

        assertThatThrownBy(() -> jdbcTemplate.update(
                "update contents set listening_cover_media_id = ? where id = ?",
                listeningCoverMediaId,
                audioStoryId))
                .isInstanceOf(DataIntegrityViolationException.class);
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
    void narrationIsRejectedForNonStoryContent() throws Exception {
        String accessToken = authenticateAdmin();
        Long audioMediaId = registerAudioAsset("/content/meditation/not-a-story/audio.mp3");

        MvcResult createResult = mockMvc.perform(post("/api/admin/contents")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "type": "MEDITATION",
                                  "externalKey": "not-a-story",
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
                                  "title": "Meditation",
                                  "bodyText": "Breathe.",
                                  "audioMediaId": %d,
                                  "status": "DRAFT",
                                  "processingStatus": "PENDING",
                                  "narration": {
                                    "audioMediaId": %d,
                                    "durationMinutes": 4
                                  }
                                }
                                """.formatted(audioMediaId, audioMediaId)))
                .andExpect(status().isBadRequest());
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
