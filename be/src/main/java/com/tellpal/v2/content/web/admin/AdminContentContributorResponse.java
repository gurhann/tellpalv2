package com.tellpal.v2.content.web.admin;

import com.tellpal.v2.content.application.ContributorManagementResults.ContentContributorRecord;

public record AdminContentContributorResponse(
        Long assignmentId,
        Long contentId,
        Long contributorId,
        String contributorDisplayName,
        String role,
        String languageCode,
        String creditName,
        int sortOrder) {

    static AdminContentContributorResponse from(ContentContributorRecord record) {
        return new AdminContentContributorResponse(
                record.assignmentId(),
                record.contentId(),
                record.contributorId(),
                record.contributorDisplayName(),
                record.role().name(),
                record.languageCode() == null ? null : record.languageCode().value(),
                record.creditName(),
                record.sortOrder());
    }
}
