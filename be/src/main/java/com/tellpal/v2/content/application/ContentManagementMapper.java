package com.tellpal.v2.content.application;

import com.tellpal.v2.content.application.ContentManagementResults.ContentLocalizationRecord;
import com.tellpal.v2.content.application.ContentManagementResults.StoryPageLocalizationRecord;
import com.tellpal.v2.content.application.ContentManagementResults.StoryPageRecord;
import com.tellpal.v2.content.application.ContributorManagementResults.ContentContributorRecord;
import com.tellpal.v2.content.application.ContributorManagementResults.ContributorRecord;
import com.tellpal.v2.content.domain.ContentContributor;
import com.tellpal.v2.content.domain.ContentLocalization;
import com.tellpal.v2.content.domain.Contributor;
import com.tellpal.v2.content.domain.StoryPage;
import com.tellpal.v2.content.domain.StoryPageLocalization;

final class ContentManagementMapper {

    private ContentManagementMapper() {
    }

    static ContentLocalizationRecord toLocalizationRecord(Long contentId, ContentLocalization localization) {
        return new ContentLocalizationRecord(
                contentId,
                localization.getLanguageCode(),
                localization.getTitle(),
                localization.getDescription(),
                localization.getBodyText(),
                localization.getCoverMediaId(),
                localization.getAudioMediaId(),
                localization.getDurationMinutes(),
                localization.getStatus(),
                localization.getProcessingStatus(),
                localization.getPublishedAt(),
                localization.isVisibleToMobile());
    }

    static StoryPageRecord toStoryPageRecord(Long contentId, StoryPage storyPage) {
        return new StoryPageRecord(
                contentId,
                storyPage.getPageNumber(),
                storyPage.getIllustrationMediaId(),
                storyPage.getLocalizations().size());
    }

    static StoryPageLocalizationRecord toStoryPageLocalizationRecord(
            Long contentId,
            int pageNumber,
            StoryPageLocalization localization) {
        return new StoryPageLocalizationRecord(
                contentId,
                pageNumber,
                localization.getLanguageCode(),
                localization.getBodyText(),
                localization.getAudioMediaId());
    }

    static ContributorRecord toContributorRecord(Contributor contributor) {
        Long contributorId = contributor.getId();
        if (contributorId == null || contributorId <= 0) {
            throw new IllegalStateException("Contributor must be persisted before mapping");
        }
        return new ContributorRecord(contributorId, contributor.getDisplayName());
    }

    static ContentContributorRecord toContentContributorRecord(Long contentId, ContentContributor assignment) {
        Long contributorId = assignment.getContributor().getId();
        if (contributorId == null || contributorId <= 0) {
            throw new IllegalStateException("Contributor must be persisted before mapping");
        }
        return new ContentContributorRecord(
                contentId,
                contributorId,
                assignment.getContributor().getDisplayName(),
                assignment.getRole(),
                assignment.getLanguageCode(),
                assignment.getCreditName(),
                assignment.getSortOrder());
    }
}
