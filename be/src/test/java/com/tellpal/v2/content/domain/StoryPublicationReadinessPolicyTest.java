package com.tellpal.v2.content.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.tellpal.v2.shared.domain.LanguageCode;

class StoryPublicationReadinessPolicyTest {

    private final StoryPublicationReadinessPolicy policy = new StoryPublicationReadinessPolicy();

    @Test
    void reportsEveryMissingEditorialRequirementWithoutPrioritizingThem() {
        Content content = Content.create(ContentType.STORY, "story.registry-test", 5, true);
        content.upsertLocalization(
                LanguageCode.TR,
                "Eksik Hikâye",
                null,
                null,
                null,
                null,
                null,
                LocalizationStatus.DRAFT,
                ProcessingStatus.PENDING,
                null);
        content.addStoryPage(null);

        assertThat(policy.evaluate(content, LanguageCode.TR))
                .extracting(StoryPublicationBlocker::code)
                .contains(
                        StoryPublicationBlockerCode.DESCRIPTION_MISSING,
                        StoryPublicationBlockerCode.COVER_MISSING,
                        StoryPublicationBlockerCode.PAGE_LOCALIZATION_MISSING);
    }
}
