package com.tellpal.v2.category;

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
import com.tellpal.v2.asset.api.AssetRegistryApi;
import com.tellpal.v2.asset.api.AssetStorageProvider;
import com.tellpal.v2.asset.api.RegisterMediaAssetCommand;
import com.tellpal.v2.category.application.CategoryApplicationExceptions.CategoryContentTypeMismatchException;
import com.tellpal.v2.category.application.CategoryApplicationExceptions.ContentLocalizationNotPublishedException;
import com.tellpal.v2.category.application.CategoryApplicationExceptions.DuplicateCategorySlugException;
import com.tellpal.v2.category.application.CategoryCurationService;
import com.tellpal.v2.category.application.CategoryManagementCommands.AddCategoryContentCommand;
import com.tellpal.v2.category.application.CategoryManagementCommands.CategoryContentOrderAssignment;
import com.tellpal.v2.category.application.CategoryManagementCommands.CreateCategoryCommand;
import com.tellpal.v2.category.application.CategoryManagementCommands.CreateCategoryLocalizationCommand;
import com.tellpal.v2.category.application.CategoryManagementCommands.ReorderCategoryContentsCommand;
import com.tellpal.v2.category.application.CategoryManagementResults.CategoryLocalizationRecord;
import com.tellpal.v2.category.application.CategoryManagementService;
import com.tellpal.v2.category.domain.CategoryType;
import com.tellpal.v2.category.domain.LocalizationStatus;
import com.tellpal.v2.content.api.ContentReference;
import com.tellpal.v2.content.application.ContentManagementCommands.CreateContentCommand;
import com.tellpal.v2.content.application.ContentManagementCommands.CreateContentLocalizationCommand;
import com.tellpal.v2.content.application.ContentManagementService;
import com.tellpal.v2.content.domain.ContentType;
import com.tellpal.v2.content.domain.ProcessingStatus;
import com.tellpal.v2.shared.domain.LanguageCode;
import com.tellpal.v2.support.PostgresIntegrationTestBase;

@SpringBootTest
class CategoryManagementIntegrationTest extends PostgresIntegrationTestBase {

    private static final String SAMPLE_CHECKSUM =
            "00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff";

    @Autowired
    private CategoryManagementService categoryManagementService;

    @Autowired
    private CategoryCurationService categoryCurationService;

    @Autowired
    private ContentManagementService contentManagementService;

    @Autowired
    private AssetRegistryApi assetRegistryApi;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("""
                truncate table
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
    void createCategoryRejectsDuplicateSlug() {
        categoryManagementService.createCategory(new CreateCategoryCommand(
                "bedtime-stories",
                CategoryType.STORY,
                false,
                true));

        assertThatThrownBy(() -> categoryManagementService.createCategory(new CreateCategoryCommand(
                "bedtime-stories",
                CategoryType.STORY,
                true,
                true)))
                .isInstanceOf(DuplicateCategorySlugException.class);
    }

    @Test
    void createLocalizationPersistsPublishedStatusAndImageReference() {
        Long categoryId = categoryManagementService.createCategory(new CreateCategoryCommand(
                "sleep-routines",
                CategoryType.MEDITATION,
                true,
                true)).categoryId();
        Long imageMediaId = registerImageAsset("/categories/sleep-routines/cover.jpg");

        CategoryLocalizationRecord localization = categoryManagementService.createLocalization(
                new CreateCategoryLocalizationCommand(
                        categoryId,
                        LanguageCode.TR,
                        "Uyku Rutinleri",
                        "Aksam hazirlik rehberi",
                        imageMediaId,
                        LocalizationStatus.PUBLISHED,
                        Instant.parse("2026-02-01T00:00:00Z")));

        Map<String, Object> row = jdbcTemplate.queryForMap(
                """
                        select status, image_media_id, published_at
                        from category_localizations
                        where category_id = ? and language_code = ?
                        """,
                categoryId,
                LanguageCode.TR.value());

        assertThat(localization.published()).isTrue();
        assertThat(localization.imageMediaId()).isEqualTo(imageMediaId);
        assertThat(row.get("status")).isEqualTo("PUBLISHED");
        assertThat(row.get("image_media_id")).isEqualTo(imageMediaId);
        assertThat(row.get("published_at")).isNotNull();
    }

    @Test
    void addContentRejectsUnpublishedContentLocalization() {
        Long categoryId = createPublishedCategory("mindfulness", CategoryType.STORY);
        Long contentId = createContentWithLocalization("mindfulness-audio", LanguageCode.TR, false);

        assertThatThrownBy(() -> categoryCurationService.addContent(new AddCategoryContentCommand(
                categoryId,
                LanguageCode.TR,
                contentId,
                0)))
                .isInstanceOf(ContentLocalizationNotPublishedException.class);
    }

    @Test
    void curationScopesDisplayOrderByLanguageAndRejectsDuplicatesWithinSameLanguage() {
        Long categoryId = createPublishedCategory("featured-sleep", CategoryType.STORY);
        Long turkishContentId = createContentWithLocalization("sleep-star-tr", LanguageCode.TR, true);
        Long secondTurkishContentId = createContentWithLocalization("sleep-star-tr-2", LanguageCode.TR, true);
        Long englishContentId = createContentWithLocalization("sleep-star-en", LanguageCode.EN, true);

        categoryCurationService.addContent(new AddCategoryContentCommand(
                categoryId,
                LanguageCode.TR,
                turkishContentId,
                0));
        categoryCurationService.addContent(new AddCategoryContentCommand(
                categoryId,
                LanguageCode.EN,
                englishContentId,
                0));

        assertThatThrownBy(() -> categoryCurationService.addContent(new AddCategoryContentCommand(
                categoryId,
                LanguageCode.TR,
                secondTurkishContentId,
                0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("display order");

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                        select language_code, content_id, display_order
                        from category_contents
                        where category_id = ?
                        order by language_code
                        """,
                categoryId);

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).get("language_code")).isEqualTo(LanguageCode.EN.value());
        assertThat(rows.get(0).get("display_order")).isEqualTo(0);
        assertThat(rows.get(1).get("language_code")).isEqualTo(LanguageCode.TR.value());
        assertThat(rows.get(1).get("display_order")).isEqualTo(0);
    }

    @Test
    void curationRejectsContentTypeMismatch() {
        Long categoryId = createPublishedCategory("quiet-breathing", CategoryType.MEDITATION);
        Long contentId = createContentWithLocalization("story-only", LanguageCode.TR, true);

        assertThatThrownBy(() -> categoryCurationService.addContent(new AddCategoryContentCommand(
                categoryId,
                LanguageCode.TR,
                contentId,
                0)))
                .isInstanceOf(CategoryContentTypeMismatchException.class)
                .hasMessageContaining("MEDITATION")
                .hasMessageContaining("STORY");
    }

    @Test
    void reorderContentsReplacesWholeLanguageLaneInOneTransaction() {
        Long categoryId = createPublishedCategory("featured-sleep", CategoryType.STORY);
        Long firstContentId = createContentWithLocalization("sleep-star-tr", LanguageCode.TR, true);
        Long secondContentId = createContentWithLocalization("sleep-star-tr-2", LanguageCode.TR, true);

        categoryCurationService.addContent(new AddCategoryContentCommand(
                categoryId,
                LanguageCode.TR,
                firstContentId,
                0));
        categoryCurationService.addContent(new AddCategoryContentCommand(
                categoryId,
                LanguageCode.TR,
                secondContentId,
                1));

        categoryCurationService.reorderContents(new ReorderCategoryContentsCommand(
                categoryId,
                LanguageCode.TR,
                List.of(
                        new CategoryContentOrderAssignment(secondContentId, 0),
                        new CategoryContentOrderAssignment(firstContentId, 1))));

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                        select content_id, display_order
                        from category_contents
                        where category_id = ? and language_code = ?
                        order by display_order asc
                        """,
                categoryId,
                LanguageCode.TR.value());

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).get("content_id")).isEqualTo(secondContentId);
        assertThat(rows.get(0).get("display_order")).isEqualTo(0);
        assertThat(rows.get(1).get("content_id")).isEqualTo(firstContentId);
        assertThat(rows.get(1).get("display_order")).isEqualTo(1);
    }

    private Long createPublishedCategory(String slug, CategoryType type) {
        Long categoryId = categoryManagementService.createCategory(new CreateCategoryCommand(
                slug,
                type,
                false,
                true)).categoryId();
        categoryManagementService.createLocalization(new CreateCategoryLocalizationCommand(
                categoryId,
                LanguageCode.TR,
                "Kategori " + slug,
                null,
                null,
                LocalizationStatus.PUBLISHED,
                Instant.parse("2026-01-01T00:00:00Z")));
        categoryManagementService.createLocalization(new CreateCategoryLocalizationCommand(
                categoryId,
                LanguageCode.EN,
                "Category " + slug,
                null,
                null,
                LocalizationStatus.PUBLISHED,
                Instant.parse("2026-01-01T00:00:00Z")));
        return categoryId;
    }

    private Long createContentWithLocalization(String externalKey, LanguageCode languageCode, boolean published) {
        ContentReference content = contentManagementService.createContent(
                new CreateContentCommand(ContentType.STORY, externalKey, null, true));
        contentManagementService.createLocalization(new CreateContentLocalizationCommand(
                content.contentId(),
                languageCode,
                "Content " + externalKey,
                null,
                null,
                null,
                null,
                null,
                published
                        ? com.tellpal.v2.content.domain.LocalizationStatus.PUBLISHED
                        : com.tellpal.v2.content.domain.LocalizationStatus.DRAFT,
                ProcessingStatus.PENDING,
                published ? Instant.parse("2026-01-01T00:00:00Z") : null));
        return content.contentId();
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
}
