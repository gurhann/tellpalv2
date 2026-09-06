package com.tellpal.v2.content.api;

/** Admin projection of a locale-scoped story narration and its independent processing state. */
public record AdminStoryNarrationView(Long audioMediaId, Integer durationMinutes, String processingStatus,
        String processingError) { }
