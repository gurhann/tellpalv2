package com.tellpal.v2.content.api;

/** Shared lullaby playback snapshot and its single content-scoped delivery status. */
public record AdminLullabyPlaybackView(
        Long audioMediaId,
        Integer durationMinutes,
        String processingStatus,
        String processingError) {

    public AdminLullabyPlaybackView {
        if (audioMediaId == null || audioMediaId <= 0) {
            throw new IllegalArgumentException("Lullaby audio media ID must be positive");
        }
        if (durationMinutes == null || durationMinutes < 0) {
            throw new IllegalArgumentException("Lullaby duration minutes must be non-negative");
        }
        if (processingStatus != null && processingStatus.isBlank()) {
            throw new IllegalArgumentException("Processing status must not be blank");
        }
    }
}
