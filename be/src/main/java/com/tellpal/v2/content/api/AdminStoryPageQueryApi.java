package com.tellpal.v2.content.api;

import java.util.List;
import java.util.Optional;

/**
 * Admin-facing read API for story-page collections and localized page payloads.
 */
public interface AdminStoryPageQueryApi {

    /**
     * Returns the story-page collection for one parent content aggregate.
     */
    List<AdminStoryPageView> listStoryPages(Long contentId);

    /**
     * Returns one story page and its localized payloads when it exists.
     */
    Optional<AdminStoryPageView> findStoryPage(Long contentId, int pageNumber);
}
