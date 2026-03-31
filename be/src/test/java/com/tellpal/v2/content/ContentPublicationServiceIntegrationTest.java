package com.tellpal.v2.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.tellpal.v2.asset.api.AssetKind;
import com.tellpal.v2.asset.api.AssetRegistryApi;
import com.tellpal.v2.asset.api.AssetStorageProvider;
import com.tellpal.v2.asset.api.RegisterMediaAssetCommand;
import com.tellpal.v2.content.api.ContentReference;
import com.tellpal.v2.content.application.ContentManagementCommands.AddStoryPageCommand;
import com.tellpal.v2.content.application.ContentManagementCommands.CreateContentCommand;
import com.tellpal.v2.content.application.ContentManagementCommands.CreateContentLocalizationCommand;
import com.tellpal.v2.content.application.ContentManagementCommands.UpsertStoryPageLocalizationCommand;
import com.tellpal.v2.content.application.ContentManagementResults.ContentLocalizationRecord;
import com.tellpal.v2.content.application.ContentManagementService;
import com.tellpal.v2.content.application.ContentPublicationCommands.ArchiveContentLocalizationCommand;
import com.tellpal.v2.content.application.ContentPublicationCommands.PublishContentLocalizationCommand;
import com.tellpal.v2.content.application.ContentPublicationService;
import com.tellpal.v2.content.application.StoryPageManagementService;
import com.tellpal.v2.content.domain.ContentType;
import com.tellpal.v2.content.domain.LocalizationStatus;
import com.tellpal.v2.content.domain.ProcessingStatus;
import com.tellpal.v2.shared.domain.LanguageCode;
import com.tellpal.v2.support.PostgresIntegrationTestBase;

@SpringBootTest
class ContentPublicationServiceIntegrationTest extends PostgresIntegrationTestBase {

    private static final String SAMPLE_CHECKSUM =
            "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789";

    @Autowired
    private ContentManagementService contentManagementService;

    @Autowired
    private StoryPageManagementService storyPageManagementService;

    @Autowired
    private ContentPublicationService contentPublicationService;

    @Autowired
    private AssetRegistryApi assetRegistryApi;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("""
                truncate table
                    story_page_localizations,
                    story_pages,
                    content_localizations,
                    contents,
                    media_assets
                restart identity cascade
                """);
    }

    @Test
    void storyLocalizationRequiresAudioAndTextOnEveryPageBeforePublication() {
        ContentReference content = contentManagementService.createContent(
                new CreateContentCommand(ContentType.STORY, "moonlight-publication", 5, true));
        Long coverMediaId = registerImageAsset("/content/story/moonlight/cover.jpg");
        Long illustrationMediaId = registerImageAsset("/content/story/moonlight/page-1.jpg");
        Long pageAudioMediaId = registerAudioAsset("/content/story/moonlight/page-1.mp3");

        contentManagementService.createLocalization(new CreateContentLocalizationCommand(
                content.contentId(),
                LanguageCode.TR,
                "Ay Isigi",
                "Gece masali",
                null,
                coverMediaId,
                null,
                null,
                LocalizationStatus.DRAFT,
                ProcessingStatus.PENDING,
                null));

        storyPageManagementService.addStoryPage(new AddStoryPageCommand(content.contentId(), 1));
        storyPageManagementService.upsertStoryPageLocalization(new UpsertStoryPageLocalizationCommand(
                content.contentId(),
                1,
                LanguageCode.TR,
                "Bir varmis bir yokmus.",
                null,
                illustrationMediaId));

        assertThatThrownBy(() -> contentPublicationService.publishLocalization(
                new PublishContentLocalizationCommand(
                        content.contentId(),
                        LanguageCode.TR,
                        Instant.parse("2026-03-17T09:00:00Z"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("audio media");

        storyPageManagementService.upsertStoryPageLocalization(new UpsertStoryPageLocalizationCommand(
                content.contentId(),
                1,
                LanguageCode.TR,
                "Bir varmis bir yokmus.",
                pageAudioMediaId,
                illustrationMediaId));

        ContentLocalizationRecord published = contentPublicationService.publishLocalization(
                new PublishContentLocalizationCommand(
                        content.contentId(),
                        LanguageCode.TR,
                        Instant.parse("2026-03-17T09:00:00Z")));

        assertThat(published.status()).isEqualTo(LocalizationStatus.PUBLISHED);
        assertThat(published.publishedAt()).isEqualTo(Instant.parse("2026-03-17T09:00:00Z"));
    }

    @Test
    void archiveLocalizationMarksPublishedLocalizationAsArchived() {
        ContentReference content = contentManagementService.createContent(
                new CreateContentCommand(ContentType.MEDITATION, "calm-mind-publication", 8, true));
        Long coverMediaId = registerImageAsset("/content/meditation/calm-mind/cover.jpg");
        Long audioMediaId = registerAudioAsset("/content/meditation/calm-mind/audio.mp3");
        Instant publishedAt = Instant.parse("2026-03-17T10:00:00Z");

        contentManagementService.createLocalization(new CreateContentLocalizationCommand(
                content.contentId(),
                LanguageCode.EN,
                "Calm Mind",
                "Bedtime meditation",
                "Breathe slowly.",
                coverMediaId,
                audioMediaId,
                9,
                LocalizationStatus.DRAFT,
                ProcessingStatus.COMPLETED,
                null));

        contentPublicationService.publishLocalization(new PublishContentLocalizationCommand(
                content.contentId(),
                LanguageCode.EN,
                publishedAt));

        ContentLocalizationRecord archived = contentPublicationService.archiveLocalization(
                new ArchiveContentLocalizationCommand(content.contentId(), LanguageCode.EN));

        Map<String, Object> row = jdbcTemplate.queryForMap(
                """
                        select status, published_at
                        from content_localizations
                        where content_id = ? and language_code = ?
                        """,
                content.contentId(),
                LanguageCode.EN.value());

        assertThat(archived.status()).isEqualTo(LocalizationStatus.ARCHIVED);
        assertThat(archived.publishedAt()).isEqualTo(publishedAt);
        assertThat(row.get("status")).isEqualTo("ARCHIVED");
        assertThat(row.get("published_at")).isNotNull();
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
