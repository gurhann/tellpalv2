package com.tellpal.v2.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.tellpal.v2.content.api.ContentReference;
import com.tellpal.v2.content.api.ResolvedContentFreeAccessSet;
import com.tellpal.v2.content.application.ContentApplicationExceptions.ContentFreeAccessAlreadyExistsException;
import com.tellpal.v2.content.application.ContentApplicationExceptions.ContentLocalizationNotFoundException;
import com.tellpal.v2.content.application.ContentFreeAccessCommands.GrantContentFreeAccessCommand;
import com.tellpal.v2.content.application.ContentFreeAccessCommands.RevokeContentFreeAccessCommand;
import com.tellpal.v2.content.application.ContentFreeAccessResults.ContentFreeAccessRecord;
import com.tellpal.v2.content.application.ContentFreeAccessService;
import com.tellpal.v2.content.application.ContentManagementCommands.CreateContentCommand;
import com.tellpal.v2.content.application.ContentManagementCommands.CreateContentLocalizationCommand;
import com.tellpal.v2.content.application.ContentManagementService;
import com.tellpal.v2.content.domain.ContentType;
import com.tellpal.v2.content.domain.LocalizationStatus;
import com.tellpal.v2.content.domain.ProcessingStatus;
import com.tellpal.v2.shared.domain.LanguageCode;
import com.tellpal.v2.support.PostgresIntegrationTestBase;

@SpringBootTest
class ContentFreeAccessServiceIntegrationTest extends PostgresIntegrationTestBase {

    @Autowired
    private ContentManagementService contentManagementService;

    @Autowired
    private ContentFreeAccessService contentFreeAccessService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("""
                truncate table
                    content_free_access,
                    story_page_localizations,
                    story_pages,
                    content_localizations,
                    contents
                restart identity cascade
                """);
    }

    @Test
    void resolveFreeAccessFallsBackToDefaultOnlyWhenRequestedKeyIsUnknown() {
        ContentReference defaultTrContent = createStoryWithLocalization("default-tr-story", LanguageCode.TR);
        ContentReference experimentEnContent = createStoryWithLocalization("experiment-en-story", LanguageCode.EN);

        contentFreeAccessService.grantFreeAccess(new GrantContentFreeAccessCommand(
                "default",
                defaultTrContent.contentId(),
                LanguageCode.TR));
        contentFreeAccessService.grantFreeAccess(new GrantContentFreeAccessCommand(
                "experiment_set_a",
                experimentEnContent.contentId(),
                LanguageCode.EN));

        ResolvedContentFreeAccessSet defaultSet = contentFreeAccessService.resolveFreeAccess(LanguageCode.TR, null);
        ResolvedContentFreeAccessSet fallbackSet = contentFreeAccessService.resolveFreeAccess(LanguageCode.TR, "missing_key");
        ResolvedContentFreeAccessSet explicitExperimentSet = contentFreeAccessService.resolveFreeAccess(
                LanguageCode.TR,
                "experiment_set_a");

        assertThat(defaultSet.accessKey()).isEqualTo("default");
        assertThat(defaultSet.contentIds()).contains(defaultTrContent.contentId());
        assertThat(fallbackSet.accessKey()).isEqualTo("default");
        assertThat(fallbackSet.contentIds()).contains(defaultTrContent.contentId());
        assertThat(explicitExperimentSet.accessKey()).isEqualTo("experiment_set_a");
        assertThat(explicitExperimentSet.contentIds()).isEmpty();
    }

    @Test
    void grantRevokeAndDuplicateRulesRespectLocalizedEntries() {
        ContentReference content = createStoryWithLocalization("localized-free-story", LanguageCode.TR);

        assertThatThrownBy(() -> contentFreeAccessService.grantFreeAccess(new GrantContentFreeAccessCommand(
                "default",
                content.contentId(),
                LanguageCode.EN)))
                .isInstanceOf(ContentLocalizationNotFoundException.class);

        ContentFreeAccessRecord record = contentFreeAccessService.grantFreeAccess(new GrantContentFreeAccessCommand(
                "default",
                content.contentId(),
                LanguageCode.TR));

        assertThat(contentFreeAccessService.isContentFree(content.contentId(), LanguageCode.TR, null)).isTrue();
        assertThat(contentFreeAccessService.isContentFree(content.contentId(), LanguageCode.TR, "missing_key")).isTrue();
        assertThat(contentFreeAccessService.listFreeAccessEntries("default"))
                .extracting(ContentFreeAccessRecord::freeAccessId)
                .containsExactly(record.freeAccessId());

        assertThatThrownBy(() -> contentFreeAccessService.grantFreeAccess(new GrantContentFreeAccessCommand(
                "default",
                content.contentId(),
                LanguageCode.TR)))
                .isInstanceOf(ContentFreeAccessAlreadyExistsException.class);

        contentFreeAccessService.revokeFreeAccess(new RevokeContentFreeAccessCommand(
                "default",
                content.contentId(),
                LanguageCode.TR));

        Integer remainingRows = jdbcTemplate.queryForObject(
                "select count(*) from content_free_access where content_id = ?",
                Integer.class,
                content.contentId());

        assertThat(remainingRows).isZero();
        assertThat(contentFreeAccessService.isContentFree(content.contentId(), LanguageCode.TR, null)).isFalse();
    }

    private ContentReference createStoryWithLocalization(String externalKey, LanguageCode languageCode) {
        ContentReference content = contentManagementService.createContent(
                new CreateContentCommand(ContentType.STORY, externalKey, 4, true));
        contentManagementService.createLocalization(new CreateContentLocalizationCommand(
                content.contentId(),
                languageCode,
                "Story " + externalKey,
                "Localized description",
                null,
                null,
                null,
                null,
                LocalizationStatus.DRAFT,
                ProcessingStatus.PENDING,
                null));
        return content;
    }
}
