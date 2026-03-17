package com.tellpal.v2.asset.web.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import com.tellpal.v2.asset.api.AssetKind;
import com.tellpal.v2.asset.api.AssetProcessingApi;
import com.tellpal.v2.asset.api.AssetProcessingCommands.FailAssetProcessingCommand;
import com.tellpal.v2.asset.api.AssetProcessingCommands.StartAssetProcessingCommand;
import com.tellpal.v2.asset.api.AssetRegistryApi;
import com.tellpal.v2.asset.api.AssetStorageProvider;
import com.tellpal.v2.asset.api.RegisterMediaAssetCommand;
import com.tellpal.v2.content.api.ContentReference;
import com.tellpal.v2.content.application.ContentManagementCommands.CreateContentCommand;
import com.tellpal.v2.content.application.ContentManagementCommands.CreateContentLocalizationCommand;
import com.tellpal.v2.content.application.ContentManagementService;
import com.tellpal.v2.content.domain.ContentType;
import com.tellpal.v2.content.domain.LocalizationStatus;
import com.tellpal.v2.content.domain.ProcessingStatus;
import com.tellpal.v2.shared.domain.LanguageCode;
import com.tellpal.v2.support.AdminApiIntegrationTestSupport;

@SpringBootTest(properties = "tellpal.asset.processing.poller.enabled=false")
@AutoConfigureMockMvc
class AssetProcessingAdminIntegrationTest extends AdminApiIntegrationTestSupport {

    private static final String SAMPLE_CHECKSUM =
            "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789";

    @Autowired
    private ContentManagementService contentManagementService;

    @Autowired
    private AssetRegistryApi assetRegistryApi;

    @Autowired
    private AssetProcessingApi assetProcessingApi;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("""
                truncate table
                    admin_refresh_tokens,
                    admin_user_roles,
                    admin_users,
                    asset_processing,
                    content_localizations,
                    contents,
                    media_assets
                restart identity cascade
                """);
    }

    @Test
    void scheduleStatusAndRetryEndpointsWorkForFailedProcessing() throws Exception {
        ContentReference content = contentManagementService.createContent(
                new CreateContentCommand(ContentType.MEDITATION, "night-breeze", 7, true));
        Long coverSourceAssetId = registerImageAsset("/content/meditation/night-breeze/tr/original/cover.jpg");
        Long audioSourceAssetId = registerAudioAsset("/content/meditation/night-breeze/tr/original/audio.mp3");
        contentManagementService.createLocalization(new CreateContentLocalizationCommand(
                content.contentId(),
                LanguageCode.TR,
                "Night Breeze",
                "Evening relaxation",
                "Listen to the breeze.",
                coverSourceAssetId,
                audioSourceAssetId,
                10,
                LocalizationStatus.PUBLISHED,
                ProcessingStatus.PENDING,
                Instant.parse("2026-01-01T00:00:00Z")));

        String accessToken = authenticateAdmin();

        mockMvc.perform(post("/api/admin/media-processing")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "contentId": %d,
                                  "languageCode": "tr",
                                  "contentType": "MEDITATION",
                                  "externalKey": "%s",
                                  "coverSourceAssetId": %d,
                                  "audioSourceAssetId": %d
                                }
                                """.formatted(
                                        content.contentId(),
                                        content.externalKey(),
                                        coverSourceAssetId,
                                        audioSourceAssetId)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"));

        mockMvc.perform(get("/api/admin/media-processing/{contentId}/localizations/{languageCode}",
                        content.contentId(),
                        "tr")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));

        assetProcessingApi.start(new StartAssetProcessingCommand(content.contentId(), LanguageCode.TR));
        assetProcessingApi.fail(new FailAssetProcessingCommand(
                content.contentId(),
                LanguageCode.TR,
                "MANUAL_FAILURE",
                "forced failure"));

        mockMvc.perform(post("/api/admin/media-processing/{contentId}/localizations/{languageCode}/retry",
                        content.contentId(),
                        "tr")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "contentType": "MEDITATION",
                                  "externalKey": "%s",
                                  "coverSourceAssetId": %d,
                                  "audioSourceAssetId": %d
                                }
                                """.formatted(
                                        content.externalKey(),
                                        coverSourceAssetId,
                                        audioSourceAssetId)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.externalKey").value(content.externalKey()));
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
