package com.tellpal.v2.content;

import static org.assertj.core.api.Assertions.assertThat;

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
import com.tellpal.v2.content.api.ContentLookupApi;
import com.tellpal.v2.content.api.ContentReference;
import com.tellpal.v2.content.application.ContentManagementCommands.AddStoryPageCommand;
import com.tellpal.v2.content.application.ContentManagementCommands.CreateContentCommand;
import com.tellpal.v2.content.application.ContentManagementCommands.CreateContentLocalizationCommand;
import com.tellpal.v2.content.application.ContentManagementCommands.RemoveStoryPageCommand;
import com.tellpal.v2.content.application.ContentManagementResults.ContentLocalizationRecord;
import com.tellpal.v2.content.application.ContentManagementResults.StoryPageRecord;
import com.tellpal.v2.content.application.ContentManagementService;
import com.tellpal.v2.content.application.ContributorManagementCommands.AssignContentContributorCommand;
import com.tellpal.v2.content.application.ContributorManagementCommands.CreateContributorCommand;
import com.tellpal.v2.content.application.ContributorManagementResults.ContributorRecord;
import com.tellpal.v2.content.application.ContributorManagementService;
import com.tellpal.v2.content.application.StoryPageManagementService;
import com.tellpal.v2.content.domain.ContentType;
import com.tellpal.v2.content.domain.ContributorRole;
import com.tellpal.v2.content.domain.LocalizationStatus;
import com.tellpal.v2.content.domain.ProcessingStatus;
import com.tellpal.v2.shared.domain.LanguageCode;
import com.tellpal.v2.support.PostgresIntegrationTestBase;

@SpringBootTest
class ContentManagementIntegrationTest extends PostgresIntegrationTestBase {

    private static final String SAMPLE_CHECKSUM =
            "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789";

    @Autowired
    private ContentManagementService contentManagementService;

    @Autowired
    private StoryPageManagementService storyPageManagementService;

    @Autowired
    private ContributorManagementService contributorManagementService;

    @Autowired
    private ContentLookupApi contentLookupApi;

    @Autowired
    private AssetRegistryApi assetRegistryApi;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("""
                truncate table
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
    void storyPageOperationsKeepPageCountInSync() {
        ContentReference content = contentManagementService.createContent(
                new CreateContentCommand(ContentType.STORY, "moonlight-story", 5, true));

        storyPageManagementService.addStoryPage(new AddStoryPageCommand(content.contentId(), null));
        storyPageManagementService.addStoryPage(new AddStoryPageCommand(content.contentId(), 1));
        storyPageManagementService.removeStoryPage(new RemoveStoryPageCommand(content.contentId(), 1));

        Integer pageCount = jdbcTemplate.queryForObject(
                "select page_count from contents where id = ?",
                Integer.class,
                content.contentId());

        assertThat(pageCount).isEqualTo(1);
        assertThat(contentLookupApi.findById(content.contentId()))
                .hasValueSatisfying(reference -> assertThat(reference.pageCount()).isEqualTo(1));
    }

    @Test
    void insertingAndRemovingStoryPagesKeepsPageNumbersContiguous() {
        ContentReference content = contentManagementService.createContent(
                new CreateContentCommand(ContentType.STORY, "renumber-story", 5, true));

        storyPageManagementService.addStoryPage(new AddStoryPageCommand(content.contentId(), null));
        storyPageManagementService.addStoryPage(new AddStoryPageCommand(content.contentId(), null));
        StoryPageRecord inserted = storyPageManagementService.addStoryPage(
                new AddStoryPageCommand(content.contentId(), 1));

        assertThat(inserted.pageNumber()).isEqualTo(2);
        assertThat(jdbcTemplate.queryForList(
                "select page_number from story_pages where content_id = ? order by page_number",
                Integer.class,
                content.contentId())).containsExactly(1, 2, 3);

        storyPageManagementService.removeStoryPage(new RemoveStoryPageCommand(content.contentId(), 2));

        assertThat(jdbcTemplate.queryForList(
                "select page_number from story_pages where content_id = ? order by page_number",
                Integer.class,
                content.contentId())).containsExactly(1, 2);
    }

    @Test
    void publishedLocalizationBecomesVisibleOnlyAfterProcessingCompletes() {
        ContentReference content = contentManagementService.createContent(
                new CreateContentCommand(ContentType.MEDITATION, "calm-mind", 8, true));
        Long coverMediaId = registerImageAsset("/content/meditation/calm-mind/cover.jpg");
        Long audioMediaId = registerAudioAsset("/content/meditation/calm-mind/audio.mp3");

        ContentLocalizationRecord created = contentManagementService.createLocalization(
                new CreateContentLocalizationCommand(
                        content.contentId(),
                        LanguageCode.TR,
                        "Sakin Zihin",
                        "Uyku oncesi meditasyon",
                        "Nefesine odaklan.",
                        coverMediaId,
                        audioMediaId,
                        12,
                        LocalizationStatus.PUBLISHED,
                        ProcessingStatus.PENDING,
                        Instant.parse("2026-01-01T00:00:00Z")));

        ContentLocalizationRecord ready = contentManagementService.markAsReady(content.contentId(), LanguageCode.TR);
        Map<String, Object> row = jdbcTemplate.queryForMap(
                """
                        select status, processing_status, published_at
                        from content_localizations
                        where content_id = ? and language_code = ?
                        """,
                content.contentId(),
                LanguageCode.TR.value());

        assertThat(created.visibleToMobile()).isFalse();
        assertThat(ready.visibleToMobile()).isTrue();
        assertThat(ready.processingStatus()).isEqualTo(ProcessingStatus.COMPLETED);
        assertThat(row.get("status")).isEqualTo("PUBLISHED");
        assertThat(row.get("processing_status")).isEqualTo("COMPLETED");
        assertThat(row.get("published_at")).isNotNull();
    }

    @Test
    void contributorAssignmentsArePersistedWithLanguageScopedOrdering() {
        ContentReference content = contentManagementService.createContent(
                new CreateContentCommand(ContentType.STORY, "forest-walk", 4, true));
        ContributorRecord author = contributorManagementService.createContributor(
                new CreateContributorCommand("Elif Yilmaz"));

        contributorManagementService.assignContentContributor(new AssignContentContributorCommand(
                content.contentId(),
                author.contributorId(),
                ContributorRole.AUTHOR,
                LanguageCode.TR,
                "E. Yilmaz",
                0));

        Map<String, Object> row = jdbcTemplate.queryForMap(
                """
                        select contributor_id, role, language_code, credit_name, sort_order
                        from content_contributors
                        where content_id = ?
                        """,
                content.contentId());

        assertThat(row.get("contributor_id")).isEqualTo(author.contributorId());
        assertThat(row.get("role")).isEqualTo("AUTHOR");
        assertThat(row.get("language_code")).isEqualTo(LanguageCode.TR.value());
        assertThat(row.get("credit_name")).isEqualTo("E. Yilmaz");
        assertThat(row.get("sort_order")).isEqualTo(0);
    }

    @Test
    void listContributorsReturnsNewestFirstAndHonorsLimit() throws Exception {
        ContributorRecord first = contributorManagementService.createContributor(
                new CreateContributorCommand("Aylin"));
        Thread.sleep(10L);
        ContributorRecord second = contributorManagementService.createContributor(
                new CreateContributorCommand("Baris"));
        Thread.sleep(10L);
        ContributorRecord third = contributorManagementService.createContributor(
                new CreateContributorCommand("Cem"));

        assertThat(contributorManagementService.listContributors(2))
                .extracting(ContributorRecord::contributorId)
                .containsExactly(third.contributorId(), second.contributorId());
        assertThat(contributorManagementService.listContributors(5))
                .extracting(ContributorRecord::contributorId)
                .containsExactly(third.contributorId(), second.contributorId(), first.contributorId());
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
