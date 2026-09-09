package com.tellpal.v2.content.web.admin;

import static org.assertj.core.api.Assertions.assertThat;
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
                    lullaby_playbacks,
                    asset_processing,
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
    void canonicalAudioStoryCreateIsRejected() throws Exception {
        String accessToken = authenticateAdmin();

        mockMvc.perform(post("/api/admin/contents")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "type": "AUDIO_STORY",
                                  "externalKey": "legacy-audio-story",
                                  "active": true
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void canonicalAudioStoryRegistryFilterIsRejected() throws Exception {
        String accessToken = authenticateAdmin();

        mockMvc.perform(get("/api/admin/content-registry")
                        .header("Authorization", "Bearer " + accessToken)
                        .queryParam("type", "AUDIO_STORY"))
                .andExpect(status().isBadRequest());
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
                .andExpect(jsonPath("$.errorCode").value("asset_media_type_mismatch"))
                .andExpect(jsonPath("$.fieldErrors.listeningCoverMediaId").isNotEmpty());

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
                .andExpect(jsonPath("$.errorCode").value("asset_not_found"))
                .andExpect(jsonPath("$.fieldErrors.listeningCoverMediaId").isNotEmpty());

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
    void listeningCoverOwnershipIsEnforcedForLullabyAndMeditation() throws Exception {
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

        mockMvc.perform(put("/api/admin/contents/{contentId}", meditationId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "externalKey": "shared-cover-meditation",
                                  "active": true,
                                  "listeningCoverMediaId": %d
                                }
                                """.formatted(listeningCoverMediaId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.listeningCoverMediaId").value(listeningCoverMediaId));

        assertThat(jdbcTemplate.queryForObject(
                "select listening_cover_media_id from contents where id = ?", Long.class, lullabyId))
                .isEqualTo(listeningCoverMediaId);
        assertThat(jdbcTemplate.queryForObject(
                "select listening_cover_media_id from contents where id = ?", Long.class, meditationId))
                .isEqualTo(listeningCoverMediaId);
    }

    @Test
    void lullabyListingCoverIsIndependentAndTypeScoped() throws Exception {
        String accessToken = authenticateAdmin();
        Long listingCover = registerImageAsset("/content/lullaby/listing.jpg");
        Long playbackCover = registerGifImageAsset("/content/lullaby/playback.gif");
        Long replacementListingCover = registerImageAsset("/content/lullaby/listing-2.jpg");
        Long audioAsset = registerAudioAsset("/content/lullaby/not-an-image.mp3");
        Long lullabyId = readPayload(mockMvc.perform(post("/api/admin/contents")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("{\"type\":\"LULLABY\",\"externalKey\":\"lullaby-covers\",\"active\":true}"))
                .andExpect(status().isCreated()).andReturn()).get("contentId").asLong();

        mockMvc.perform(put("/api/admin/contents/{contentId}", lullabyId)
                        .header("Authorization", "Bearer " + accessToken).contentType("application/json")
                        .content("{\"externalKey\":\"lullaby-covers\",\"active\":true,\"listingCoverMediaId\":%d,\"listeningCoverMediaId\":%d}"
                                .formatted(listingCover, playbackCover)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.listingCoverMediaId").value(listingCover))
                .andExpect(jsonPath("$.listeningCoverMediaId").value(playbackCover));

        mockMvc.perform(put("/api/admin/contents/{contentId}", lullabyId)
                        .header("Authorization", "Bearer " + accessToken).contentType("application/json")
                        .content("{\"externalKey\":\"lullaby-covers\",\"active\":true,\"listingCoverMediaId\":%d,\"listeningCoverMediaId\":%d}"
                                .formatted(audioAsset, playbackCover)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.listingCoverMediaId").isNotEmpty());

        mockMvc.perform(put("/api/admin/contents/{contentId}", lullabyId)
                        .header("Authorization", "Bearer " + accessToken).contentType("application/json")
                        .content("{\"externalKey\":\"lullaby-covers\",\"active\":true,\"listingCoverMediaId\":%d,\"listeningCoverMediaId\":null}"
                                .formatted(replacementListingCover)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.listingCoverMediaId").value(replacementListingCover))
                .andExpect(jsonPath("$.listeningCoverMediaId").value(nullValue()));

        mockMvc.perform(post("/api/admin/contents/{contentId}/localizations/tr", lullabyId)
                        .header("Authorization", "Bearer " + accessToken).contentType("application/json")
                        .content("{\"title\":\"Ninni\",\"status\":\"DRAFT\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/admin/contents/{contentId}/localizations/en", lullabyId)
                        .header("Authorization", "Bearer " + accessToken).contentType("application/json")
                        .content("{\"title\":\"Lullaby\",\"status\":\"DRAFT\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(put("/api/admin/contents/{contentId}", lullabyId)
                        .header("Authorization", "Bearer " + accessToken).contentType("application/json")
                        .content("{\"externalKey\":\"lullaby-covers\",\"active\":true,\"listingCoverMediaId\":null,\"listeningCoverMediaId\":%d}"
                                .formatted(playbackCover)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.listingCoverMediaId").value(nullValue()))
                .andExpect(jsonPath("$.listeningCoverMediaId").value(playbackCover));
        mockMvc.perform(get("/api/admin/contents/{contentId}", lullabyId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.listingCoverMediaId").value(nullValue()))
                .andExpect(jsonPath("$.listeningCoverMediaId").value(playbackCover))
                .andExpect(jsonPath("$.localizations.length()").value(2));

        Long meditationId = readPayload(mockMvc.perform(post("/api/admin/contents")
                        .header("Authorization", "Bearer " + accessToken).contentType("application/json")
                        .content("{\"type\":\"MEDITATION\",\"externalKey\":\"meditation-covers\",\"active\":true}"))
                .andExpect(status().isCreated()).andReturn()).get("contentId").asLong();
        mockMvc.perform(put("/api/admin/contents/{contentId}", meditationId)
                        .header("Authorization", "Bearer " + accessToken).contentType("application/json")
                        .content("{\"externalKey\":\"meditation-covers\",\"active\":true,\"listingCoverMediaId\":%d}".formatted(listingCover)))
                .andExpect(status().isBadRequest());
        assertThat(jdbcTemplate.queryForObject("select listing_cover_media_id from contents where id = ?", Long.class, meditationId))
                .isNull();
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
                                  "type": "MEDITATION",
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
        Long listeningCoverMediaId = registerImageAsset("/content/story/moonlight/listening-cover.jpg");

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

        mockMvc.perform(put("/api/admin/contents/{contentId}", activeContentId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "externalKey": "moonlight-story",
                                  "ageRange": 5,
                                  "active": true,
                                  "listeningCoverMediaId": %d
                                }
                                """.formatted(listeningCoverMediaId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.listeningCoverMediaId").value(listeningCoverMediaId));

        MvcResult inactiveContentResult = mockMvc.perform(post("/api/admin/contents")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "type": "MEDITATION",
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
                .andExpect(jsonPath("$[0].listeningCoverMediaId").value(listeningCoverMediaId))
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

    @Test
    void lullabyUsesOneSharedPlaybackAcrossTitleOnlyLocalizations() throws Exception {
        String accessToken = authenticateAdmin();
        Long audioMediaId = registerAudioAsset("/content/lullaby/night-sky/audio.mp3");

        MvcResult createResult = mockMvc.perform(post("/api/admin/contents")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "type": "LULLABY",
                                  "externalKey": "night-sky-lullaby",
                                  "active": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        Long contentId = readPayload(createResult).get("contentId").asLong();

        for (String languageCode : new String[] {"tr", "en"}) {
            mockMvc.perform(post("/api/admin/contents/{contentId}/localizations/{languageCode}", contentId, languageCode)
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType("application/json")
                            .content("""
                                    {
                                      "title": "Gece Ninnisi",
                                      "status": "DRAFT"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.processingStatus").value("PENDING"))
                    .andExpect(jsonPath("$.audioMediaId").value(nullValue()))
                    .andExpect(jsonPath("$.coverMediaId").value(nullValue()));
        }

        mockMvc.perform(put("/api/admin/contents/{contentId}/playback", contentId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "audioMediaId": %d,
                                  "durationMinutes": 10
                                }
                                """.formatted(audioMediaId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contentId").value(contentId))
                .andExpect(jsonPath("$.audioMediaId").value(audioMediaId))
                .andExpect(jsonPath("$.durationMinutes").value(10))
                .andExpect(jsonPath("$.processingStatus").value("PENDING"));

        mockMvc.perform(put("/api/admin/contents/{contentId}/instruments", contentId)
                        .param("languageCode", "tr")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                { "instrumentCodes": ["CELESTA", "BELL"] }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/contents/{contentId}", contentId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playback.audioMediaId").value(audioMediaId))
                .andExpect(jsonPath("$.playback.durationMinutes").value(10))
                .andExpect(jsonPath("$.playback.instruments[0].code").value("CELESTA"))
                .andExpect(jsonPath("$.playback.instruments[0].displayOrder").value(0))
                .andExpect(jsonPath("$.playback.instruments[1].code").value("BELL"))
                .andExpect(jsonPath("$.playback.instruments[1].displayOrder").value(1))
                .andExpect(jsonPath("$.localizations[0].audioMediaId").value(nullValue()))
                .andExpect(jsonPath("$.localizations[1].audioMediaId").value(nullValue()));

        mockMvc.perform(put("/api/admin/contents/{contentId}/localizations/tr", contentId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "title": "Gece Ninnisi",
                                  "bodyText": "Bu alan desteklenmez",
                                  "status": "DRAFT"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void lullabyPlaybackRejectsMissingAndNonAudioAssetsWithoutPersisting() throws Exception {
        String accessToken = authenticateAdmin();
        Long imageMediaId = registerImageAsset("/content/lullaby/night-sky/not-audio.jpg");

        MvcResult createResult = mockMvc.perform(post("/api/admin/contents")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "type": "LULLABY",
                                  "externalKey": "invalid-lullaby-playback",
                                  "active": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        Long contentId = readPayload(createResult).get("contentId").asLong();

        mockMvc.perform(put("/api/admin/contents/{contentId}/playback", contentId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "audioMediaId": %d,
                                  "durationMinutes": 10
                                }
                                """.formatted(imageMediaId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("asset_media_type_mismatch"));

        mockMvc.perform(put("/api/admin/contents/{contentId}/playback", contentId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "audioMediaId": 99999,
                                  "durationMinutes": 10
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("asset_not_found"));

        mockMvc.perform(get("/api/admin/contents/{contentId}", contentId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playback").value(nullValue()));
    }

    @Test
    void lullabyInstrumentsUseLocaleCatalogLabelsAndPreserveSelectionOnInvalidUpdate() throws Exception {
        String accessToken = authenticateAdmin();

        MvcResult createResult = mockMvc.perform(post("/api/admin/contents")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "type": "LULLABY",
                                  "externalKey": "catalog-lullaby",
                                  "active": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        Long contentId = readPayload(createResult).get("contentId").asLong();

        mockMvc.perform(get("/api/admin/instrument-catalog")
                        .param("languageCode", "tr")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(8))
                .andExpect(jsonPath("$[0].code").value("BELL"))
                .andExpect(jsonPath("$[0].displayName").value("Bell"))
                .andExpect(jsonPath("$[1].code").value("CELESTA"))
                .andExpect(jsonPath("$[1].displayName").value("Çelesta"))
                .andExpect(jsonPath("$[2].code").value("GLOCKENSPIEL"))
                .andExpect(jsonPath("$[2].displayName").value("Glockenspiel"))
                .andExpect(jsonPath("$[3].code").value("HARP"))
                .andExpect(jsonPath("$[3].displayName").value("Arp"))
                .andExpect(jsonPath("$[4].code").value("RHODES"))
                .andExpect(jsonPath("$[4].displayName").value("Rhodes"))
                .andExpect(jsonPath("$[5].code").value("STRING_ORCHESTRA"))
                .andExpect(jsonPath("$[5].displayName").value("Yaylı Orkestra"))
                .andExpect(jsonPath("$[6].code").value("VIBRAPHONE"))
                .andExpect(jsonPath("$[6].displayName").value("Vibrafon"))
                .andExpect(jsonPath("$[7].code").value("VIOLIN"))
                .andExpect(jsonPath("$[7].displayName").value("Keman"));

        mockMvc.perform(put("/api/admin/contents/{contentId}/instruments", contentId)
                        .param("languageCode", "tr")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                { "instrumentCodes": ["BELL", "CELESTA"] }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("BELL"))
                .andExpect(jsonPath("$[0].displayName").value("Bell"))
                .andExpect(jsonPath("$[0].displayOrder").value(0))
                .andExpect(jsonPath("$[1].code").value("CELESTA"))
                .andExpect(jsonPath("$[1].displayOrder").value(1));

        mockMvc.perform(put("/api/admin/contents/{contentId}/instruments", contentId)
                        .param("languageCode", "tr")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                { "instrumentCodes": [" celesta ", "bell"] }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("CELESTA"))
                .andExpect(jsonPath("$[1].code").value("BELL"));

        mockMvc.perform(put("/api/admin/contents/{contentId}/instruments", contentId)
                        .param("languageCode", "tr")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                { "instrumentCodes": ["BELL", "BELL"] }
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/admin/contents/{contentId}/instruments", contentId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                { "instrumentCodes": ["BELL", "UNKNOWN"] }
                                """))
                        .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/admin/contents/{contentId}/instruments", contentId)
                        .param("languageCode", "en")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                { "instrumentCodes": ["CELESTA", "BELL"] }
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/admin/contents/{contentId}/instruments", contentId)
                        .param("languageCode", "tr")
                        .header("Authorization", "Bearer " + accessToken))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$[0].code").value("CELESTA"))
                .andExpect(jsonPath("$[0].displayName").value("Çelesta"))
                .andExpect(jsonPath("$[0].displayOrder").value(0))
                .andExpect(jsonPath("$[1].code").value("BELL"))
                .andExpect(jsonPath("$[1].displayName").value("Bell"))
                .andExpect(jsonPath("$[1].displayOrder").value(1));

        mockMvc.perform(get("/api/admin/contents/{contentId}/instruments", contentId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("CELESTA"))
                .andExpect(jsonPath("$[0].displayOrder").value(0))
                .andExpect(jsonPath("$[1].code").value("BELL"))
                .andExpect(jsonPath("$[1].displayOrder").value(1));

        MvcResult storyResult = mockMvc.perform(post("/api/admin/contents")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "type": "STORY",
                                  "externalKey": "instrument-story",
                                  "active": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        Long storyId = readPayload(storyResult).get("contentId").asLong();
        mockMvc.perform(put("/api/admin/contents/{contentId}/instruments", storyId)
                        .param("languageCode", "tr")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                { "instrumentCodes": ["BELL"] }
                                """))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/admin/contents/{contentId}/instruments", storyId)
                        .param("languageCode", "tr")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest());
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

    private Long registerGifImageAsset(String objectPath) {
        return assetRegistryApi.register(new RegisterMediaAssetCommand(
                AssetStorageProvider.LOCAL_STUB, objectPath, AssetKind.ORIGINAL_IMAGE,
                "image/gif", 1024L, SAMPLE_CHECKSUM)).assetId();
    }
}
