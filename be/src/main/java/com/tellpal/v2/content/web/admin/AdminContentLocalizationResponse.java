package com.tellpal.v2.content.web.admin;

import java.time.Instant;

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
        boolean visibleToMobile) {

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
                record.visibleToMobile());
    }
}
