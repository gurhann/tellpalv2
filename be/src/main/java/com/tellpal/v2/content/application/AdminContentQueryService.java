package com.tellpal.v2.content.application;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tellpal.v2.content.api.AdminContentQueryApi;
import com.tellpal.v2.content.api.AdminContentView;
import com.tellpal.v2.content.domain.ContentRepository;

/**
 * Read-only application service for admin content queries.
 *
 * <p>The service returns metadata plus localized content snapshots needed by CMS list and detail
 * screens, including inactive content items.
 */
@Service
@Transactional(readOnly = true)
public class AdminContentQueryService implements AdminContentQueryApi {

    private final ContentRepository contentRepository;

    public AdminContentQueryService(ContentRepository contentRepository) {
        this.contentRepository = contentRepository;
    }

    /**
     * Returns all content aggregates for CMS operators with their localization snapshots.
     */
    @Override
    public List<AdminContentView> listContents() {
        return contentRepository.findAllForAdminRead().stream()
                .map(ContentAdminQueryMapper::toView)
                .toList();
    }

    /**
     * Returns one content aggregate and its localization snapshots when it exists.
     */
    @Override
    public Optional<AdminContentView> findContent(Long contentId) {
        return contentRepository.findByIdForAdminRead(requireContentId(contentId))
                .map(ContentAdminQueryMapper::toView);
    }

    private static Long requireContentId(Long contentId) {
        if (contentId == null || contentId <= 0) {
            throw new IllegalArgumentException("Content ID must be positive");
        }
        return contentId;
    }
}
