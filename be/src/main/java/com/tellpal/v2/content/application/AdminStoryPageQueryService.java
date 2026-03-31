package com.tellpal.v2.content.application;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tellpal.v2.content.api.AdminStoryPageQueryApi;
import com.tellpal.v2.content.api.AdminStoryPageView;
import com.tellpal.v2.content.application.ContentApplicationExceptions.ContentNotFoundException;
import com.tellpal.v2.content.domain.Content;
import com.tellpal.v2.content.domain.ContentRepository;

/**
 * Read-only application service for admin story-page queries.
 *
 * <p>The service returns page metadata plus localized story-page payloads for STORY content
 * aggregates only.
 */
@Service
@Transactional(readOnly = true)
public class AdminStoryPageQueryService implements AdminStoryPageQueryApi {

    private final ContentRepository contentRepository;

    public AdminStoryPageQueryService(ContentRepository contentRepository) {
        this.contentRepository = contentRepository;
    }

    /**
     * Returns the story-page collection for one STORY content aggregate.
     */
    @Override
    public List<AdminStoryPageView> listStoryPages(Long contentId) {
        Content content = loadStoryContent(contentId);
        Long requiredContentId = requireContentId(content);
        return content.getStoryPages().stream()
                .sorted(java.util.Comparator.comparingInt(storyPage -> storyPage.getPageNumber()))
                .map(storyPage -> StoryPageAdminQueryMapper.toView(requiredContentId, storyPage))
                .toList();
    }

    /**
     * Returns one story page and its localized payloads when it exists under a STORY content
     * aggregate.
     */
    @Override
    public Optional<AdminStoryPageView> findStoryPage(Long contentId, int pageNumber) {
        Content content = loadStoryContent(contentId);
        Long requiredContentId = requireContentId(content);
        return content.findStoryPage(requirePageNumber(pageNumber))
                .map(storyPage -> StoryPageAdminQueryMapper.toView(requiredContentId, storyPage));
    }

    private Content loadStoryContent(Long contentId) {
        Content content = contentRepository.findByIdForStoryPageAdminRead(requireContentId(contentId))
                .orElseThrow(() -> new ContentNotFoundException(contentId));
        if (!content.getType().supportsStoryPages()) {
            throw new IllegalStateException("Story pages can only be managed for STORY content");
        }
        return content;
    }

    private static Long requireContentId(Long contentId) {
        if (contentId == null || contentId <= 0) {
            throw new IllegalArgumentException("Content ID must be positive");
        }
        return contentId;
    }

    private static Long requireContentId(Content content) {
        Long contentId = content.getId();
        if (contentId == null || contentId <= 0) {
            throw new IllegalStateException("Content must be persisted before story-page admin query mapping");
        }
        return contentId;
    }

    private static int requirePageNumber(int pageNumber) {
        if (pageNumber <= 0) {
            throw new IllegalArgumentException("Story page number must be positive");
        }
        return pageNumber;
    }
}
