package com.tellpal.v2.content.web.admin;

import java.util.Set;

import com.tellpal.v2.content.application.ContributorManagementResults.ContributorRecord;
import com.tellpal.v2.content.domain.ContributorRole;

public record AdminContributorResponse(Long contributorId, String displayName, Set<ContributorRole> roles) {

    static AdminContributorResponse from(ContributorRecord record) {
        return new AdminContributorResponse(record.contributorId(), record.displayName(), record.roles());
    }
}
