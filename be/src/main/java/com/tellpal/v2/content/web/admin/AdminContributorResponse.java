package com.tellpal.v2.content.web.admin;

import com.tellpal.v2.content.application.ContributorManagementResults.ContributorRecord;

public record AdminContributorResponse(Long contributorId, String displayName) {

    static AdminContributorResponse from(ContributorRecord record) {
        return new AdminContributorResponse(record.contributorId(), record.displayName());
    }
}
