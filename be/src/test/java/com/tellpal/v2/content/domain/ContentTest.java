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
        content.upsertLocalization(LanguageCode.TR, "Masal", null, null, null, null, null,
                LocalizationStatus.DRAFT, ProcessingStatus.PENDING, null);
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
        content.upsertLocalization(LanguageCode.TR, "Masal", null, null, null, null, null,
                LocalizationStatus.DRAFT, ProcessingStatus.PENDING, null);
        content.upsertLocalization(LanguageCode.EN, "Story", null, null, null, null, null,
                LocalizationStatus.DRAFT, ProcessingStatus.PENDING, null);
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
        content.upsertLocalization(LanguageCode.TR, "Masal", null, null, null, null, null,
                LocalizationStatus.DRAFT, ProcessingStatus.PENDING, null);
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
    void updateContributorPreservesIdentityAndAppendsWhenMovingGroups() {
        Content content = Content.create(ContentType.STORY, "story-edit-contributor", 6, true);
        Contributor author = Contributor.create("Alice", Set.of(ContributorRole.AUTHOR, ContributorRole.MUSICIAN));
        ReflectionTestUtils.setField(author, "id", 1L);
        Contributor musician = Contributor.create("Bob", Set.of(ContributorRole.AUTHOR, ContributorRole.MUSICIAN));
        ReflectionTestUtils.setField(musician, "id", 2L);

        ContentContributor first = content.assignContributor(author, ContributorRole.AUTHOR, null, "A", 0);
        ContentContributor second = content.assignContributor(musician, ContributorRole.MUSICIAN, null, "B", 0);
        ReflectionTestUtils.setField(first, "id", 101L);
        ReflectionTestUtils.setField(second, "id", 102L);

        ContentContributor updated = content.updateContributor(
                101L, ContributorRole.MUSICIAN, null, " Alice Updated ");

        assertThat(updated).isSameAs(first);
        assertThat(updated.getContributor()).isSameAs(author);
        assertThat(updated.getId()).isEqualTo(101L);
        assertThat(updated.getCreditName()).isEqualTo("Alice Updated");
        assertThat(updated.getSortOrder()).isEqualTo(1);
        assertThat(second.getSortOrder()).isEqualTo(0);
    }

    @Test
    void updateContributorRejectsUnknownLanguageAndDuplicateAssignment() {
        Content content = Content.create(ContentType.STORY, "story-edit-validation", 6, true);
        content.upsertLocalization(LanguageCode.TR, "Masal", null, null, null, null, null,
                LocalizationStatus.DRAFT, ProcessingStatus.PENDING, null);
        Contributor first = persistedContributor(1L, "Alice");

        ContentContributor firstAssignment = content.assignContributor(first, ContributorRole.AUTHOR, null, null);
        ContentContributor secondAssignment = content.assignContributor(first, ContributorRole.AUTHOR, LanguageCode.TR, null);
        ReflectionTestUtils.setField(firstAssignment, "id", 101L);
        ReflectionTestUtils.setField(secondAssignment, "id", 102L);

        assertThatThrownBy(() -> content.updateContributor(
                101L, ContributorRole.AUTHOR, LanguageCode.EN, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("language must exist");

        assertThatThrownBy(() -> content.updateContributor(
                101L, ContributorRole.AUTHOR, LanguageCode.TR, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void updateContributorKeepsGroupOrderAndNormalizesBlankCreditName() {
        Content content = Content.create(ContentType.STORY, "story-edit-credit-only", 6, true);
        Contributor contributor = persistedContributor(1L, "Alice");
        ContentContributor assignment = content.assignContributor(
                contributor, ContributorRole.AUTHOR, null, "Original");
        ReflectionTestUtils.setField(assignment, "id", 101L);

        ContentContributor updated = content.updateContributor(
                101L, ContributorRole.AUTHOR, null, "   ");

        assertThat(updated.getCreditName()).isNull();
        assertThat(updated.getSortOrder()).isZero();
        assertThat(updated.getRole()).isEqualTo(ContributorRole.AUTHOR);
        assertThat(updated.getLanguageCode()).isNull();
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
