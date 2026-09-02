package com.tellpal.v2.content.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.tellpal.v2.content.api.AdminContentRegistryReadiness;
import com.tellpal.v2.content.api.ContentApiType;
import com.tellpal.v2.content.application.ContentRegistryReadRepository;
import com.tellpal.v2.content.application.ContentRegistryReadRepository.RegistryQuery;
import com.tellpal.v2.support.PostgresIntegrationTestBase;
import com.tellpal.v2.shared.domain.LanguageCode;

@SpringBootTest
class JdbcContentRegistryReadRepositoryIntegrationTest extends PostgresIntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ContentRegistryReadRepository contentRegistryReadRepository;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("""
                truncate table
                    story_page_localizations,
                    story_pages,
                    content_localizations,
                    contents
                restart identity cascade
                """);
    }

    @Test
    void appliesReadinessFilteringAndPaginationBeforeLoadingSelectedPageSnapshots() {
        long actionRequiredId = insertContent("STORY", "registry-action", true);
        long laterActionRequiredId = insertContent("STORY", "registry-action-later", true);
        long readyToPublishId = insertContent("STORY", "registry-ready", true);
        insertLocalization(readyToPublishId, "DRAFT", "COMPLETED", "Hazir hikaye", "Aciklama", 100L);
        long readyPageId = insertStoryPage(readyToPublishId, 1);
        insertStoryPageLocalization(readyPageId, "Hazir sayfa", 101L, 102L);

        long publishedId = insertContent("AUDIO_STORY", "registry-published", true);
        insertLocalization(publishedId, "PUBLISHED", "COMPLETED", "Yayindaki ses", "Aciklama", 200L);

        ContentRegistryReadRepository.RegistryPage actionRequiredPage = contentRegistryReadRepository.findPage(
                new RegistryQuery(
                        LanguageCode.TR,
                        null,
                        AdminContentRegistryReadiness.ACTION_REQUIRED,
                        "registry",
                        0,
                        1));

        assertThat(actionRequiredPage.totalItems()).isEqualTo(2);
        assertThat(actionRequiredPage.candidates())
                .extracting(ContentRegistryReadRepository.RegistryCandidate::contentId)
                .containsExactly(laterActionRequiredId);

        ContentRegistryReadRepository.RegistryPage secondActionRequiredPage = contentRegistryReadRepository.findPage(
                new RegistryQuery(
                        LanguageCode.TR,
                        null,
                        AdminContentRegistryReadiness.ACTION_REQUIRED,
                        "registry",
                        1,
                        1));

        assertThat(secondActionRequiredPage.candidates())
                .extracting(ContentRegistryReadRepository.RegistryCandidate::contentId)
                .containsExactly(actionRequiredId);

        ContentRegistryReadRepository.RegistryPage readyPage = contentRegistryReadRepository.findPage(
                new RegistryQuery(
                        LanguageCode.TR,
                        ContentApiType.STORY,
                        AdminContentRegistryReadiness.READY_TO_PUBLISH,
                        "ready",
                        0,
                        1));

        assertThat(readyPage.totalItems()).isEqualTo(1);
        assertThat(readyPage.candidates())
                .extracting(ContentRegistryReadRepository.RegistryCandidate::contentId)
                .containsExactly(readyToPublishId);
        assertThat(contentRegistryReadRepository.findSnapshots(List.of(readyToPublishId), LanguageCode.TR))
                .hasSize(1)
                .allSatisfy(row -> {
                    assertThat(row.contentId()).isEqualTo(readyToPublishId);
                    assertThat(row.storyPageNumber()).isEqualTo(1);
                    assertThat(row.storyPageBodyText()).isEqualTo("Hazir sayfa");
                });

        ContentRegistryReadRepository.RegistryPage publishedPage = contentRegistryReadRepository.findPage(
                new RegistryQuery(
                        LanguageCode.TR,
                        ContentApiType.AUDIO_STORY,
                        AdminContentRegistryReadiness.PUBLISHED,
                        "published",
                        0,
                        1));

        assertThat(publishedPage.totalItems()).isEqualTo(1);
        assertThat(publishedPage.candidates())
                .extracting(ContentRegistryReadRepository.RegistryCandidate::contentId)
                .containsExactly(publishedId);
    }

    private long insertContent(String type, String externalKey, boolean active) {
        return jdbcTemplate.queryForObject("""
                        insert into contents (type, external_key, is_active, page_count)
                        values (?, ?, ?, case when ? = 'STORY' then 0 else null end)
                        returning id
                        """,
                Long.class,
                type,
                externalKey,
                active,
                type);
    }

    private void insertLocalization(
            long contentId,
            String status,
            String processingStatus,
            String title,
            String description,
            long coverMediaId) {
        jdbcTemplate.update("""
                        insert into content_localizations
                            (content_id, language_code, title, description, cover_media_id, status, processing_status, published_at)
                        values (?, 'tr', ?, ?, ?, ?, ?, case when ? = 'PUBLISHED' then now() else null end)
                        """,
                contentId,
                title,
                description,
                coverMediaId,
                status,
                processingStatus,
                status);
    }

    private long insertStoryPage(long contentId, int pageNumber) {
        return jdbcTemplate.queryForObject("""
                        insert into story_pages (content_id, page_number)
                        values (?, ?)
                        returning id
                        """,
                Long.class,
                contentId,
                pageNumber);
    }

    private void insertStoryPageLocalization(long storyPageId, String bodyText, long audioMediaId, long illustrationMediaId) {
        jdbcTemplate.update("""
                        insert into story_page_localizations
                            (story_page_id, language_code, body_text, audio_media_id, illustration_media_id)
                        values (?, 'tr', ?, ?, ?)
                        """,
                storyPageId,
                bodyText,
                audioMediaId,
                illustrationMediaId);
    }
}
