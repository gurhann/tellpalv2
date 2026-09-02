package com.tellpal.v2.content.domain;

/**
 * One editor-facing condition that prevents a story localization from publishing.
 */
public record StoryPublicationBlocker(StoryPublicationBlockerCode code, Integer pageNumber) {
}
