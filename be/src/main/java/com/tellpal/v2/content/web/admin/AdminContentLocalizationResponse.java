package com.tellpal.v2.content.web.admin;

import java.time.Instant;

import com.tellpal.v2.content.api.AdminContentLocalizationView;
import com.tellpal.v2.content.application.ContentManagementResults.ContentLocalizationRecord;

public record AdminContentLocalizationResponse(
        Long contentId,
        String languageCode,
        String title,
        String description,
        String bodyText,
        Long coverMediaId,
        Long audioMediaId,
        Integer durationMinutes,
        String status,
        String processingStatus,
        Instant publishedAt,
        boolean visibleToMobile,
        AdminStoryNarrationResponse narration) {

    public AdminContentLocalizationResponse(Long contentId, String languageCode, String title,
            String description, String bodyText, Long coverMediaId, Long audioMediaId,
            Integer durationMinutes, String status, String processingStatus, Instant publishedAt,
            boolean visibleToMobile) {
        this(contentId, languageCode, title, description, bodyText, coverMediaId, audioMediaId,
                durationMinutes, status, processingStatus, publishedAt, visibleToMobile, null);
    }

    static AdminContentLocalizationResponse from(ContentLocalizationRecord record) {
        return new AdminContentLocalizationResponse(
                record.contentId(),
                record.languageCode().value(),
                record.title(),
                record.description(),
                record.bodyText(),
                record.coverMediaId(),
                record.audioMediaId(),
                record.durationMinutes(),
                record.status().name(),
                record.processingStatus().name(),
                record.publishedAt(),
                record.visibleToMobile(), record.narration() == null ? null : new AdminStoryNarrationResponse(
                        record.narration().audioMediaId(), record.narration().durationMinutes(), null, null));
    }

    static AdminContentLocalizationResponse from(AdminContentLocalizationView view) {
        return new AdminContentLocalizationResponse(
                view.contentId(),
                view.languageCode().value(),
                view.title(),
                view.description(),
                view.bodyText(),
                view.coverMediaId(),
                view.audioMediaId(),
                view.durationMinutes(),
                view.status(),
                view.processingStatus(),
                view.publishedAt(),
                view.visibleToMobile(), view.narration() == null ? null : new AdminStoryNarrationResponse(
                        view.narration().audioMediaId(), view.narration().durationMinutes(),
                        view.narration().processingStatus(), view.narration().processingError()));
    }
}

record AdminStoryNarrationResponse(Long audioMediaId, Integer durationMinutes, String processingStatus,
        String processingError) { }
