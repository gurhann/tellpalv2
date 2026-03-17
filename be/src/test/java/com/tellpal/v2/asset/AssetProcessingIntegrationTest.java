package com.tellpal.v2.asset;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.tellpal.v2.asset.api.AssetKind;
import com.tellpal.v2.asset.api.AssetProcessingApi;
import com.tellpal.v2.asset.api.AssetProcessingCommands.ScheduleAssetProcessingCommand;
import com.tellpal.v2.asset.api.AssetProcessingCommands.StartAssetProcessingCommand;
import com.tellpal.v2.asset.api.AssetProcessingContentType;
import com.tellpal.v2.asset.api.AssetProcessingRecord;
import com.tellpal.v2.asset.api.AssetRegistryApi;
import com.tellpal.v2.asset.api.AssetStorageProvider;
import com.tellpal.v2.asset.api.RegisterMediaAssetCommand;
import com.tellpal.v2.asset.infrastructure.processing.AssetProcessingJobExecutor;
import com.tellpal.v2.content.api.ContentReference;
import com.tellpal.v2.content.application.ContentManagementCommands.CreateContentCommand;
import com.tellpal.v2.content.application.ContentManagementCommands.CreateContentLocalizationCommand;
import com.tellpal.v2.content.application.ContentManagementCommands.AddStoryPageCommand;
import com.tellpal.v2.content.application.ContentManagementService;
import com.tellpal.v2.content.application.StoryPageManagementService;
import com.tellpal.v2.content.domain.ContentType;
import com.tellpal.v2.content.domain.LocalizationStatus;
import com.tellpal.v2.content.domain.ProcessingStatus;
import com.tellpal.v2.shared.domain.LanguageCode;
import com.tellpal.v2.support.PostgresIntegrationTestBase;

@SpringBootTest
class AssetProcessingIntegrationTest extends PostgresIntegrationTestBase {

    private static final String SAMPLE_CHECKSUM =
            "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789";

    @Autowired
    private AssetRegistryApi assetRegistryApi;

    @Autowired
    private AssetProcessingApi assetProcessingApi;

    @Autowired
    private AssetProcessingJobExecutor assetProcessingJobExecutor;

    @Autowired
    private ContentManagementService contentManagementService;

    @Autowired
    private StoryPageManagementService storyPageManagementService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("""
                truncate table
                    asset_processing,
                    story_page_localizations,
                    story_pages,
                    content_localizations,
                    contents,
                    media_assets
                restart identity cascade
                """);
    }

    @Test
    void storyProcessingGeneratesCoverVariantsAndStoryPackagesAndMarksLocalizationReady() {
        ContentReference content = contentManagementService.createContent(
                new CreateContentCommand(ContentType.STORY, "moonlight-story", 5, true));
        Long coverSourceAssetId = registerImageAsset("/content/story/moonlight-story/tr/original/cover.jpg");
        Long pageIllustrationAssetId = registerImageAsset("/content/story/moonlight-story/tr/original/page-1.jpg");

        contentManagementService.createLocalization(new CreateContentLocalizationCommand(
                content.contentId(),
                LanguageCode.TR,
                "Moonlight Story",
                "Dreamy bedtime story",
                null,
                coverSourceAssetId,
                null,
                null,
                LocalizationStatus.PUBLISHED,
                ProcessingStatus.PENDING,
                Instant.parse("2026-01-01T00:00:00Z")));
        storyPageManagementService.addStoryPage(new AddStoryPageCommand(
                content.contentId(),
                1,
                pageIllustrationAssetId));

        AssetProcessingRecord started = scheduleAndStart(new ScheduleAssetProcessingCommand(
                content.contentId(),
                LanguageCode.TR,
                AssetProcessingContentType.STORY,
                content.externalKey(),
                coverSourceAssetId,
                null,
                1));

        assetProcessingJobExecutor.process(started);

        Map<String, Object> localizationRow = jdbcTemplate.queryForMap(
                """
                        select processing_status
                        from content_localizations
                        where content_id = ? and language_code = ?
                        """,
                content.contentId(),
                LanguageCode.TR.value());

        List<String> generatedKinds = jdbcTemplate.queryForList(
                """
                        select kind
                        from media_assets
                        where object_path like ?
                        order by kind
                        """,
                String.class,
                "/content/story/moonlight-story/tr/%");

        assertThat(localizationRow.get("processing_status")).isEqualTo("COMPLETED");
        assertThat(generatedKinds).contains(
                "THUMBNAIL_PHONE",
                "THUMBNAIL_TABLET",
                "DETAIL_PHONE",
                "DETAIL_TABLET",
                "CONTENT_ZIP_PART1",
                "CONTENT_ZIP_PART2");
    }

    @Test
    void nonStoryProcessingGeneratesOptimizedAudioAndSinglePackage() {
        ContentReference content = contentManagementService.createContent(
                new CreateContentCommand(ContentType.MEDITATION, "calm-mind", 8, true));
        Long coverSourceAssetId = registerImageAsset("/content/meditation/calm-mind/tr/original/cover.jpg");
        Long audioSourceAssetId = registerAudioAsset("/content/meditation/calm-mind/tr/original/audio.mp3");

        contentManagementService.createLocalization(new CreateContentLocalizationCommand(
                content.contentId(),
                LanguageCode.TR,
                "Calm Mind",
                "Guided meditation",
                "Breathe in and out.",
                coverSourceAssetId,
                audioSourceAssetId,
                12,
                LocalizationStatus.PUBLISHED,
                ProcessingStatus.PENDING,
                Instant.parse("2026-01-01T00:00:00Z")));

        AssetProcessingRecord started = scheduleAndStart(new ScheduleAssetProcessingCommand(
                content.contentId(),
                LanguageCode.TR,
                AssetProcessingContentType.MEDITATION,
                content.externalKey(),
                coverSourceAssetId,
                audioSourceAssetId,
                null));

        assetProcessingJobExecutor.process(started);

        List<String> generatedKinds = jdbcTemplate.queryForList(
                """
                        select kind
                        from media_assets
                        where object_path like ?
                        order by kind
                        """,
                String.class,
                "/content/meditation/calm-mind/tr/%");

        assertThat(generatedKinds).contains(
                "THUMBNAIL_PHONE",
                "THUMBNAIL_TABLET",
                "DETAIL_PHONE",
                "DETAIL_TABLET",
                "OPTIMIZED_AUDIO",
                "CONTENT_ZIP");
        assertThat(assetProcessingApi.findByLocalization(content.contentId(), LanguageCode.TR))
                .hasValueSatisfying(record -> assertThat(record.status().name()).isEqualTo("COMPLETED"));
    }

    private AssetProcessingRecord scheduleAndStart(ScheduleAssetProcessingCommand command) {
        assetProcessingApi.schedule(command);
        return assetProcessingApi.start(new StartAssetProcessingCommand(command.contentId(), command.languageCode()));
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
