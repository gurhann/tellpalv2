package com.tellpal.v2.content.web.mobile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

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
import com.tellpal.v2.category.application.CategoryCurationService;
import com.tellpal.v2.category.application.CategoryManagementCommands.AddCategoryContentCommand;
import com.tellpal.v2.category.application.CategoryManagementCommands.CreateCategoryCommand;
import com.tellpal.v2.category.application.CategoryManagementCommands.CreateCategoryLocalizationCommand;
import com.tellpal.v2.category.application.CategoryManagementService;
import com.tellpal.v2.category.api.CategoryReference;
import com.tellpal.v2.category.domain.CategoryType;
import com.tellpal.v2.content.api.ContentReference;
import com.tellpal.v2.content.application.ContentFreeAccessCommands.GrantContentFreeAccessCommand;
import com.tellpal.v2.content.application.ContentFreeAccessService;
import com.tellpal.v2.content.application.ContentManagementCommands.AddStoryPageCommand;
import com.tellpal.v2.content.application.ContentManagementCommands.CreateContentCommand;
import com.tellpal.v2.content.application.ContentManagementCommands.CreateContentLocalizationCommand;
import com.tellpal.v2.content.application.ContentManagementCommands.UpsertStoryPageLocalizationCommand;
import com.tellpal.v2.content.application.ContentManagementService;
import com.tellpal.v2.content.application.StoryPageManagementService;
import com.tellpal.v2.content.domain.ContentType;
import com.tellpal.v2.content.domain.LocalizationStatus;
import com.tellpal.v2.content.domain.ProcessingStatus;
import com.tellpal.v2.shared.domain.LanguageCode;
import com.tellpal.v2.support.PostgresIntegrationTestBase;

@SpringBootTest
@AutoConfigureMockMvc
class PublicDeliveryIntegrationTest extends PostgresIntegrationTestBase {

    private static final Instant PUBLISHED_AT = Instant.parse("2026-03-17T09:00:00Z");
    private static final String SAMPLE_CHECKSUM =
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
    private ContentFreeAccessService contentFreeAccessService;

    @Autowired
    private CategoryManagementService categoryManagementService;

    @Autowired
    private CategoryCurationService categoryCurationService;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("""
                truncate table
                    asset_processing,
                    content_free_access,
                    category_contents,
                    category_localizations,
                    categories,
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
    void contentEndpointsApplyLanguageVisibilityAndDefaultFreeKeyFallback() throws Exception {
        ContentSeed readyStory = createReadyStory("moonlight-story", LanguageCode.TR, "Ay Isigi");
        ContentSeed readyMeditation = createReadyMeditation("calm-mind", LanguageCode.TR, "Sakin Zihin");
        createIncompleteStory("hidden-story", LanguageCode.TR, "Gizli Hikaye");
        createReadyStory("english-story", LanguageCode.EN, "Moonlight");

        contentFreeAccessService.grantFreeAccess(new GrantContentFreeAccessCommand(
                "default",
                readyStory.contentId(),
                LanguageCode.TR));

        MvcResult listResult = mockMvc.perform(get("/api/contents")
                        .queryParam("lang", "tr")
                        .queryParam("freeKey", "missing-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andReturn();

        JsonNode listPayload = readPayload(listResult);
        JsonNode storyNode = findByContentId(listPayload, readyStory.contentId());
        JsonNode meditationNode = findByContentId(listPayload, readyMeditation.contentId());

        assertThat(storyNode).isNotNull();
        assertThat(meditationNode).isNotNull();
        assertThat(storyNode.get("isFree").asBoolean()).isTrue();
        assertThat(meditationNode.get("isFree").asBoolean()).isFalse();
        assertThat(storyNode.at("/assets/coverVariants/thumbnailPhone/kind").asText()).isEqualTo("THUMBNAIL_PHONE");
        assertThat(storyNode.at("/assets/packages/storyPart1/kind").asText()).isEqualTo("CONTENT_ZIP_PART1");
        assertThat(storyNode.at("/assets/packages/storyPart2/objectPath").asText()).contains("_part2.zip");

        mockMvc.perform(get("/api/contents/{contentId}", readyMeditation.contentId())
                        .queryParam("lang", "tr"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assets.packages.singlePackage.kind").value("CONTENT_ZIP"))
                .andExpect(jsonPath("$.assets.optimizedAudio.kind").value("OPTIMIZED_AUDIO"))
                .andExpect(jsonPath("$.title").value("Sakin Zihin"));

        mockMvc.perform(get("/api/contents/{contentId}/pages", readyStory.contentId())
                        .queryParam("lang", "tr"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].pageNumber").value(1))
                .andExpect(jsonPath("$[0].bodyText").value("Bir varmis bir yokmus."))
                .andExpect(jsonPath("$[0].illustration.kind").value("ORIGINAL_IMAGE"));
    }

    @Test
    void categoryEndpointsReturnPublishedCategoriesAndPreserveCuratedOrder() throws Exception {
        ContentSeed second = createReadyStory("forest-story", LanguageCode.TR, "Orman");
        ContentSeed first = createReadyStory("moonlight-story", LanguageCode.TR, "Ay Isigi");
        ContentSeed hidden = createIncompleteStory("draft-story", LanguageCode.TR, "Taslak");
        Long categoryId = createPublishedCategory("featured-sleep", CategoryType.STORY, LanguageCode.TR);

        categoryCurationService.addContent(new AddCategoryContentCommand(
                categoryId,
                LanguageCode.TR,
                second.contentId(),
                2));
        categoryCurationService.addContent(new AddCategoryContentCommand(
                categoryId,
                LanguageCode.TR,
                hidden.contentId(),
                3));
        categoryCurationService.addContent(new AddCategoryContentCommand(
                categoryId,
                LanguageCode.TR,
                first.contentId(),
                1));

        mockMvc.perform(get("/api/categories")
                        .queryParam("lang", "tr"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].slug").value("featured-sleep"))
                .andExpect(jsonPath("$[0].type").value("STORY"));

        mockMvc.perform(get("/api/categories")
                        .queryParam("lang", "tr")
                        .queryParam("type", "STORY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].slug").value("featured-sleep"));

        mockMvc.perform(get("/api/categories")
                        .queryParam("lang", "tr")
                        .queryParam("type", "MEDITATION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        MvcResult contentsResult = mockMvc.perform(get("/api/categories/{slug}/contents", "featured-sleep")
                        .queryParam("lang", "tr"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andReturn();

        JsonNode contentsPayload = readPayload(contentsResult);
        assertThat(contentsPayload.get(0).get("contentId").asLong()).isEqualTo(first.contentId());
        assertThat(contentsPayload.get(1).get("contentId").asLong()).isEqualTo(second.contentId());
        assertThat(extractContentIds(contentsPayload)).doesNotContain(hidden.contentId());
    }

    @Test
    void storyPagesResolveIllustrationPerRequestedLanguage() throws Exception {
        ContentReference content = contentManagementService.createContent(
                new CreateContentCommand(ContentType.STORY, "bilingual-story", 5, true));
        Long trCoverMediaId = registerImageAsset("/content/story/bilingual-story/tr/original/cover.jpg");
        Long enCoverMediaId = registerImageAsset("/content/story/bilingual-story/en/original/cover.jpg");
        Long trIllustrationMediaId = registerImageAsset("/content/story/bilingual-story/tr/original/page-1.jpg");
        Long enIllustrationMediaId = registerImageAsset("/content/story/bilingual-story/en/original/page-1.jpg");
        Long trAudioMediaId = registerAudioAsset("/content/story/bilingual-story/tr/original/page-1.mp3");
        Long enAudioMediaId = registerAudioAsset("/content/story/bilingual-story/en/original/page-1.mp3");

        contentManagementService.createLocalization(new CreateContentLocalizationCommand(
                content.contentId(),
                LanguageCode.TR,
                "Iki dilli hikaye",
                "Turkce aciklama",
                null,
                trCoverMediaId,
                null,
                null,
                LocalizationStatus.PUBLISHED,
                ProcessingStatus.PENDING,
                PUBLISHED_AT));
        contentManagementService.createLocalization(new CreateContentLocalizationCommand(
                content.contentId(),
                LanguageCode.EN,
                "Bilingual story",
                "English description",
                null,
                enCoverMediaId,
                null,
                null,
                LocalizationStatus.PUBLISHED,
                ProcessingStatus.PENDING,
                PUBLISHED_AT));

        storyPageManagementService.addStoryPage(new AddStoryPageCommand(content.contentId(), null));
        storyPageManagementService.upsertStoryPageLocalization(new UpsertStoryPageLocalizationCommand(
                content.contentId(),
                1,
                LanguageCode.TR,
                "Turkce sayfa metni.",
                trAudioMediaId,
                trIllustrationMediaId));
        storyPageManagementService.upsertStoryPageLocalization(new UpsertStoryPageLocalizationCommand(
                content.contentId(),
                1,
                LanguageCode.EN,
                "English page text.",
                enAudioMediaId,
                enIllustrationMediaId));

        completeProcessing(new ScheduleAssetProcessingCommand(
                content.contentId(),
                LanguageCode.TR,
                AssetProcessingContentType.STORY,
                content.externalKey(),
                trCoverMediaId,
                null,
                1));
        completeProcessing(new ScheduleAssetProcessingCommand(
                content.contentId(),
                LanguageCode.EN,
                AssetProcessingContentType.STORY,
                content.externalKey(),
                enCoverMediaId,
                null,
                1));

        mockMvc.perform(get("/api/contents/{contentId}/pages", content.contentId())
                        .queryParam("lang", "tr"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bodyText").value("Turkce sayfa metni."))
                .andExpect(jsonPath("$[0].illustration.objectPath")
                        .value("/content/story/bilingual-story/tr/original/page-1.jpg"))
                .andExpect(jsonPath("$[0].audio.objectPath")
                        .value("/content/story/bilingual-story/tr/original/page-1.mp3"));

        mockMvc.perform(get("/api/contents/{contentId}/pages", content.contentId())
                        .queryParam("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bodyText").value("English page text."))
                .andExpect(jsonPath("$[0].illustration.objectPath")
                        .value("/content/story/bilingual-story/en/original/page-1.jpg"))
                .andExpect(jsonPath("$[0].audio.objectPath")
                        .value("/content/story/bilingual-story/en/original/page-1.mp3"));
    }

    private Long createPublishedCategory(String slug, CategoryType type, LanguageCode languageCode) {
        Long imageMediaId = registerImageAsset("/categories/%s/%s/image.jpg".formatted(slug, languageCode.value()));
        CategoryReference category = categoryManagementService.createCategory(
                new CreateCategoryCommand(slug, type, false, true));
        categoryManagementService.createLocalization(new CreateCategoryLocalizationCommand(
                category.categoryId(),
                languageCode,
                "Category " + slug,
                "Public category",
                imageMediaId,
                com.tellpal.v2.category.domain.LocalizationStatus.PUBLISHED,
                PUBLISHED_AT));
        return category.categoryId();
    }

    private ContentSeed createReadyStory(String externalKey, LanguageCode languageCode, String title) {
        ContentReference content = contentManagementService.createContent(
                new CreateContentCommand(ContentType.STORY, externalKey, 5, true));
        Long coverMediaId = registerImageAsset("/content/story/%s/%s/original/cover.jpg".formatted(externalKey, languageCode.value()));
        Long illustrationMediaId = registerImageAsset("/content/story/%s/%s/original/page-1.jpg".formatted(
                externalKey,
                languageCode.value()));

        contentManagementService.createLocalization(new CreateContentLocalizationCommand(
                content.contentId(),
                languageCode,
                title,
                "Story description",
                null,
                coverMediaId,
                null,
                null,
                LocalizationStatus.PUBLISHED,
                ProcessingStatus.PENDING,
                PUBLISHED_AT));
        storyPageManagementService.addStoryPage(new AddStoryPageCommand(content.contentId(), null));
        storyPageManagementService.upsertStoryPageLocalization(new UpsertStoryPageLocalizationCommand(
                content.contentId(),
                1,
                languageCode,
                "Bir varmis bir yokmus.",
                null,
                illustrationMediaId));

        completeProcessing(new ScheduleAssetProcessingCommand(
                content.contentId(),
                languageCode,
                AssetProcessingContentType.STORY,
                content.externalKey(),
                coverMediaId,
                null,
                1));
        return new ContentSeed(content.contentId(), content.externalKey());
    }

    private ContentSeed createReadyMeditation(String externalKey, LanguageCode languageCode, String title) {
        ContentReference content = contentManagementService.createContent(
                new CreateContentCommand(ContentType.MEDITATION, externalKey, 7, true));
        Long coverMediaId = registerImageAsset("/content/meditation/%s/%s/original/cover.jpg".formatted(
                externalKey,
                languageCode.value()));
        Long audioMediaId = registerAudioAsset("/content/meditation/%s/%s/original/audio.mp3".formatted(
                externalKey,
                languageCode.value()));

        contentManagementService.createLocalization(new CreateContentLocalizationCommand(
                content.contentId(),
                languageCode,
                title,
                "Meditation description",
                "Nefesine odaklan.",
                coverMediaId,
                audioMediaId,
                12,
                LocalizationStatus.PUBLISHED,
                ProcessingStatus.PENDING,
                PUBLISHED_AT));

        completeProcessing(new ScheduleAssetProcessingCommand(
                content.contentId(),
                languageCode,
                AssetProcessingContentType.MEDITATION,
                content.externalKey(),
                coverMediaId,
                audioMediaId,
                null));
        return new ContentSeed(content.contentId(), content.externalKey());
    }

    private ContentSeed createIncompleteStory(String externalKey, LanguageCode languageCode, String title) {
        ContentReference content = contentManagementService.createContent(
                new CreateContentCommand(ContentType.STORY, externalKey, 6, true));
        Long coverMediaId = registerImageAsset("/content/story/%s/%s/original/cover.jpg".formatted(externalKey, languageCode.value()));

        contentManagementService.createLocalization(new CreateContentLocalizationCommand(
                content.contentId(),
                languageCode,
                title,
                "Incomplete story",
                null,
                coverMediaId,
                null,
                null,
                LocalizationStatus.PUBLISHED,
                ProcessingStatus.PENDING,
                PUBLISHED_AT));
        return new ContentSeed(content.contentId(), content.externalKey());
    }

    private void completeProcessing(ScheduleAssetProcessingCommand command) {
        assetProcessingApi.schedule(command);
        AssetProcessingRecord started = assetProcessingApi.start(
                new StartAssetProcessingCommand(command.contentId(), command.languageCode()));
        assetProcessingJobExecutor.process(started);
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

    private JsonNode readPayload(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }

    private JsonNode findByContentId(JsonNode payload, Long contentId) {
        for (JsonNode node : payload) {
            if (node.path("contentId").asLong() == contentId) {
                return node;
            }
        }
        return null;
    }

    private java.util.List<Long> extractContentIds(JsonNode payload) {
        java.util.List<Long> contentIds = new java.util.ArrayList<>();
        for (JsonNode node : payload) {
            contentIds.add(node.path("contentId").asLong());
        }
        return contentIds;
    }

    private record ContentSeed(Long contentId, String externalKey) {
    }
}
