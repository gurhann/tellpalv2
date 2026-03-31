package com.tellpal.v2.content.web.admin;

import com.tellpal.v2.content.api.AdminStoryPageLocalizationView;
import com.tellpal.v2.content.application.ContentManagementResults.StoryPageLocalizationRecord;

public record AdminStoryPageLocalizationResponse(
        Long contentId,
        int pageNumber,
        String languageCode,
        String bodyText,
        Long audioMediaId) {

    static AdminStoryPageLocalizationResponse from(StoryPageLocalizationRecord record) {
        return new AdminStoryPageLocalizationResponse(
                record.contentId(),
                record.pageNumber(),
                record.languageCode().value(),
                record.bodyText(),
                record.audioMediaId());
    }

    static AdminStoryPageLocalizationResponse from(AdminStoryPageLocalizationView view) {
        return new AdminStoryPageLocalizationResponse(
                view.contentId(),
                view.pageNumber(),
                view.languageCode().value(),
                view.bodyText(),
                view.audioMediaId());
    }
}
