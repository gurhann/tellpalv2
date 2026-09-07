package com.tellpal.v2.content.web.admin;

import com.tellpal.v2.content.application.ContentManagementResults.LullabyPlaybackRecord;

/** Admin response for the single content-scoped lullaby playback. */
public record AdminLullabyPlaybackResponse(
        Long contentId,
        Long audioMediaId,
        Integer durationMinutes,
        String processingStatus,
        String processingError) {

    static AdminLullabyPlaybackResponse from(LullabyPlaybackRecord record) {
        return new AdminLullabyPlaybackResponse(
                record.contentId(),
                record.audioMediaId(),
                record.durationMinutes(),
                record.processingStatus() == null ? null : record.processingStatus().name(),
                record.processingError());
    }
}
