package com.tellpal.v2.content.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.tellpal.v2.shared.domain.LanguageCode;

class ContentTest {

    @Test
    void assignContributorAppendsAtTheServerOwnedEndOfItsRoleAndLanguageGroup() {
        Content content = Content.create(ContentType.STORY, "story-contributors", 6, true);
        Contributor firstContributor = persistedContributor(1L, "Alice");
        Contributor secondContributor = persistedContributor(2L, "Bob");

        content.assignContributor(firstContributor, ContributorRole.AUTHOR, LanguageCode.TR, null, 98);
        content.assignContributor(secondContributor, ContributorRole.AUTHOR, LanguageCode.TR, null, 0);

        assertThat(content.getContributors())
                .extracting(ContentContributor::getSortOrder)
                .containsExactlyInAnyOrder(0, 1);
    }

    @Test
    void assignContributorAllowsSameSortOrderAcrossDifferentLanguages() {
        Content content = Content.create(ContentType.STORY, "story-language-scoped", 6, true);
        Contributor firstContributor = persistedContributor(1L, "Alice");
        Contributor secondContributor = persistedContributor(2L, "Bob");

        content.assignContributor(firstContributor, ContributorRole.AUTHOR, LanguageCode.TR, null, 0);
        content.assignContributor(secondContributor, ContributorRole.AUTHOR, LanguageCode.EN, null, 0);

        assertThat(content.getContributors()).hasSize(2);
    }

    @Test
    void assignContributorRejectsARoleThatTheContributorProfileDoesNotContain() {
        Content content = Content.create(ContentType.STORY, "story-role-check", 6, true);
        Contributor contributor = persistedContributor(1L, "Alice");

        assertThatThrownBy(() -> content.assignContributor(
                contributor, ContributorRole.NARRATOR, LanguageCode.TR, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requested role");
        assertThat(content.getContributors()).isEmpty();
    }

    @Test
    void unassignContributorRemovesOnlyTheExactLanguageScopedAssignment() {
        Content content = Content.create(ContentType.STORY, "story-unassign-language", 6, true);
        Contributor contributor = persistedContributor(1L, "Alice");

        content.assignContributor(contributor, ContributorRole.AUTHOR, null, null, 0);
        content.assignContributor(contributor, ContributorRole.AUTHOR, LanguageCode.TR, null, 1);

        content.unassignContributor(1L, ContributorRole.AUTHOR, LanguageCode.TR);

        assertThat(content.getContributors()).hasSize(1);
        assertThat(content.getContributors().iterator().next().getLanguageCode()).isNull();
    }

    @Test
    void unassignContributorRejectsMissingExactMatch() {
        Content content = Content.create(ContentType.STORY, "story-unassign-missing", 6, true);
        Contributor contributor = persistedContributor(1L, "Alice");

        content.assignContributor(contributor, ContributorRole.AUTHOR, null, null, 0);

        assertThatThrownBy(() -> content.unassignContributor(1L, ContributorRole.AUTHOR, LanguageCode.TR))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void unassignContributorCompactsTheRemainingRoleAndLanguageGroup() {
        Content content = Content.create(ContentType.STORY, "story-unassign-compact", 6, true);
        Contributor firstContributor = persistedContributor(1L, "Alice");
        Contributor secondContributor = persistedContributor(2L, "Bob");

        content.assignContributor(firstContributor, ContributorRole.AUTHOR, null, null, 0);
        content.assignContributor(secondContributor, ContributorRole.AUTHOR, null, null, 0);
        content.unassignContributor(1L, ContributorRole.AUTHOR, null);

        assertThat(content.getContributors()).singleElement()
                .extracting(ContentContributor::getSortOrder)
                .isEqualTo(0);
    }

    @Test
    void storyLocalizationRejectsBodyTextAndSingleAudioReference() {
        Content content = Content.create(ContentType.STORY, "story-localization-rules", 5, true);

        assertThatThrownBy(() -> content.upsertLocalization(
                LanguageCode.TR,
                "Masal",
                "Aksam rutini",
                "Sayfa disi metin",
                null,
                null,
                null,
                LocalizationStatus.DRAFT,
                ProcessingStatus.PENDING,
                null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("body text");

        assertThatThrownBy(() -> content.upsertLocalization(
                LanguageCode.TR,
                "Masal",
                "Aksam rutini",
                null,
                null,
                41L,
                null,
                LocalizationStatus.DRAFT,
                ProcessingStatus.PENDING,
                null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("audio media");
    }

    private static Contributor persistedContributor(Long contributorId, String displayName) {
        Contributor contributor = Contributor.create(displayName, Set.of(ContributorRole.AUTHOR));
        ReflectionTestUtils.setField(contributor, "id", contributorId);
        return contributor;
    }
}
