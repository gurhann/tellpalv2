package com.tellpal.v2.asset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import com.tellpal.v2.asset.api.AssetProcessingKind;
import com.tellpal.v2.asset.api.AssetProcessingCommands.ScheduleAssetProcessingCommand;
import com.tellpal.v2.asset.api.AssetProcessingCommands.RetryAssetProcessingCommand;
import com.tellpal.v2.asset.api.AssetProcessingCommands.StartAssetProcessingCommand;
import com.tellpal.v2.asset.api.AssetProcessingContentType;
import com.tellpal.v2.asset.api.AssetProcessingRecord;
import com.tellpal.v2.asset.api.AssetProcessingTarget;
import com.tellpal.v2.asset.api.AssetRegistryApi;
import com.tellpal.v2.asset.api.AssetStorageProvider;
import com.tellpal.v2.asset.api.RegisterMediaAssetCommand;
import com.tellpal.v2.asset.infrastructure.processing.AssetProcessingJobExecutor;
import com.tellpal.v2.content.api.ContentReference;
import com.tellpal.v2.content.application.ContentManagementCommands.CreateContentCommand;
import com.tellpal.v2.content.application.ContentManagementCommands.CreateContentLocalizationCommand;
import com.tellpal.v2.content.application.ContentManagementCommands.StoryNarrationCommand;
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
                null));

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
                "/test/content/story/moonlight-story/tr/%");

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
                "/test/content/meditation/calm-mind/tr/%");

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

    @Test
    void contentTargetIsIndependentFromLocalizationTargetsAndDoesNotChangeLocalizationStatus() {
        ContentReference content = contentManagementService.createContent(
                new CreateContentCommand(ContentType.MEDITATION, "shared-meditation", 8, true));
        Long coverSourceAssetId = registerImageAsset("/content/meditation/shared-meditation/shared/original/cover.jpg");
        Long audioSourceAssetId = registerAudioAsset("/content/meditation/shared-meditation/shared/original/audio.mp3");
        contentManagementService.createLocalization(new CreateContentLocalizationCommand(
                content.contentId(), LanguageCode.TR, "Shared", "Description", "Body", coverSourceAssetId,
                audioSourceAssetId, 10, LocalizationStatus.PUBLISHED, ProcessingStatus.PENDING, Instant.parse("2026-01-01T00:00:00Z")));

        AssetProcessingRecord shared = scheduleAndStart(new ScheduleAssetProcessingCommand(
                AssetProcessingTarget.content(content.contentId()), AssetProcessingContentType.MEDITATION,
                content.externalKey(), coverSourceAssetId, audioSourceAssetId, null));
        assertThat(shared.target().isContent()).isTrue();
        assertThat(assetProcessingApi.findByContent(content.contentId())).hasValueSatisfying(record ->
                assertThat(record.target()).isEqualTo(AssetProcessingTarget.content(content.contentId())));

        assetProcessingApi.fail(new com.tellpal.v2.asset.api.AssetProcessingCommands.FailAssetProcessingCommand(
                AssetProcessingTarget.content(content.contentId()), "FAILED", "shared failure"));
        assertThat(jdbcTemplate.queryForObject(
                "select processing_status from content_localizations where content_id = ? and language_code = ?",
                String.class, content.contentId(), LanguageCode.TR.value())).isEqualTo("PENDING");

        AssetProcessingRecord retried = assetProcessingApi.retry(new RetryAssetProcessingCommand(
                AssetProcessingTarget.content(content.contentId()), AssetProcessingContentType.MEDITATION,
                content.externalKey(), coverSourceAssetId, audioSourceAssetId, null));
        AssetProcessingRecord restarted = assetProcessingApi.start(new StartAssetProcessingCommand(retried.target()));
        assertThat(restarted.status().name()).isEqualTo("PROCESSING");
        assetProcessingJobExecutor.process(restarted);
        assertThat(assetProcessingApi.findByContent(content.contentId()))
                .hasValueSatisfying(record -> assertThat(record.status().name()).isEqualTo("COMPLETED"));
        assertThat(jdbcTemplate.queryForList(
                "select kind from media_assets where object_path like ?", String.class,
                "/test/content/meditation/shared-meditation/shared/%"))
                .contains("THUMBNAIL_PHONE", "THUMBNAIL_TABLET", "DETAIL_PHONE", "DETAIL_TABLET",
                        "OPTIMIZED_AUDIO", "CONTENT_ZIP");
    }

    @Test
    void databaseRejectsInvalidTargetCombinationAndCascadesLocalizationProcessing() {
        ContentReference content = contentManagementService.createContent(
                new CreateContentCommand(ContentType.STORY, "target-constraints", 5, true));
        contentManagementService.createLocalization(new CreateContentLocalizationCommand(
                content.contentId(), LanguageCode.TR, "Target constraints", null, null, null, null, null,
                LocalizationStatus.PUBLISHED, ProcessingStatus.PENDING, Instant.parse("2026-01-01T00:00:00Z")));

        assertThatThrownBy(() -> jdbcTemplate.update(
                "insert into asset_processing (content_id, target_scope, language_code) values (?, 'CONTENT', 'tr')",
                content.contentId()))
                .isInstanceOf(RuntimeException.class);

        AssetProcessingRecord localization = scheduleAndStart(new ScheduleAssetProcessingCommand(
                content.contentId(), LanguageCode.TR, AssetProcessingContentType.STORY,
                content.externalKey(), null, null, 0));
        assertThat(localization.target().isLocalization()).isTrue();

        jdbcTemplate.update("update content_localizations set language_code = 'en' where content_id = ? and language_code = ?",
                content.contentId(), LanguageCode.TR.value());
        assertThat(assetProcessingApi.findByLocalization(content.contentId(), LanguageCode.EN)).isPresent();

        jdbcTemplate.update("delete from content_localizations where content_id = ? and language_code = ?",
                content.contentId(), LanguageCode.EN.value());
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from asset_processing where content_id = ? and target_scope = 'LOCALIZATION'",
                Integer.class, content.contentId())).isZero();
    }

    @Test
    void storyNarrationHasIndependentProcessingAndProducesOnlyOptimizedAudio() {
        ContentReference content = contentManagementService.createContent(
                new CreateContentCommand(ContentType.STORY, "narrated-story", 5, true));
        Long cover = registerImageAsset("/content/story/narrated-story/tr/original/cover.jpg");
        Long narrationAudio = registerAudioAsset("/content/story/narrated-story/tr/original/narration.mp3");

        contentManagementService.createLocalization(new CreateContentLocalizationCommand(
                content.contentId(), LanguageCode.TR, "Anlatılan hikâye", "Açıklama", null, cover, null, null,
                LocalizationStatus.PUBLISHED, ProcessingStatus.PENDING, Instant.parse("2026-01-01T00:00:00Z"),
                new StoryNarrationCommand(narrationAudio, 11)));
        contentManagementService.createLocalization(new CreateContentLocalizationCommand(
                content.contentId(), LanguageCode.EN, "Narrated story", "Description", null, cover, null, null,
                LocalizationStatus.PUBLISHED, ProcessingStatus.PENDING, Instant.parse("2026-01-01T00:00:00Z")));

        AssetProcessingTarget target = AssetProcessingTarget.localization(content.contentId(), LanguageCode.TR);
        AssetProcessingRecord narration = assetProcessingApi.findByTarget(target, AssetProcessingKind.STORY_NARRATION)
                .orElseThrow();
        assertThat(assetProcessingApi.findByTarget(target, AssetProcessingKind.DELIVERY)).isEmpty();

        AssetProcessingRecord started = assetProcessingApi.start(
                new StartAssetProcessingCommand(target, AssetProcessingKind.STORY_NARRATION));
        assetProcessingJobExecutor.process(started);

        assertThat(assetProcessingApi.findByTarget(target, AssetProcessingKind.STORY_NARRATION))
                .hasValueSatisfying(record -> assertThat(record.status().name()).isEqualTo("COMPLETED"));
        assertThat(jdbcTemplate.queryForList(
                "select kind from media_assets where object_path like ?", String.class,
                "/test/content/story/narrated-story/tr/processed/%"))
                .containsExactly("OPTIMIZED_AUDIO");
        assertThat(jdbcTemplate.queryForObject(
                "select processing_status from content_localizations where content_id = ? and language_code = ?",
                String.class, content.contentId(), LanguageCode.TR.value())).isEqualTo("PENDING");
        assertThat(narration.audioSourceAssetId()).isEqualTo(narrationAudio);
    }

    private AssetProcessingRecord scheduleAndStart(ScheduleAssetProcessingCommand command) {
        assetProcessingApi.schedule(command);
        return assetProcessingApi.start(new StartAssetProcessingCommand(command.target()));
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
