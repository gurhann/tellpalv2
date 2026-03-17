package com.tellpal.v2.content.web.admin;

import com.tellpal.v2.content.application.ContentManagementResults.StoryPageRecord;

public record AdminStoryPageResponse(
        Long contentId,
        int pageNumber,
        Long illustrationMediaId,
        int localizationCount) {

    static AdminStoryPageResponse from(StoryPageRecord record) {
        return new AdminStoryPageResponse(
                record.contentId(),
                record.pageNumber(),
                record.illustrationMediaId(),
                record.localizationCount());
    }
}
