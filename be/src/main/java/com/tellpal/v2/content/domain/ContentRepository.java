package com.tellpal.v2.content.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Loads and persists {@link Content} aggregates for management and publication use cases.
 */
public interface ContentRepository {

    /**
     * Returns the aggregate identified by the internal content id when it exists.
     */
    Optional<Content> findById(Long id);

    /**
     * Loads one aggregate with its localization state for admin read flows.
     */
    Optional<Content> findByIdForAdminRead(Long id);

    /**
     * Loads aggregates with localization state for admin read flows by id.
     */
    List<Content> findAllByIdForAdminReadIn(Collection<Long> ids);

    /**
     * Loads one aggregate with its story pages and localized page payloads for admin story-page
     * read flows.
     */
    Optional<Content> findByIdForStoryPageAdminRead(Long id);

    /**
     * Loads one aggregate with contributor assignment state for admin contributor flows.
     */
    Optional<Content> findByIdForContributorAdminRead(Long id);

    /**
     * Resolves a content aggregate by the externally visible key used across modules and admin tools.
     */
    Optional<Content> findByExternalKey(String externalKey);

    /**
     * Checks whether an external key is already reserved by another content aggregate.
     */
    boolean existsByExternalKey(String externalKey);

    /**
     * Returns active aggregates that can still participate in read and curation flows.
     */
    List<Content> findAllActive();

    /**
     * Returns all aggregates with localization state for admin list and detail screens.
     */
    List<Content> findAllForAdminRead();

    /**
     * Loads the active aggregates for the provided ids and ignores inactive or missing entries.
     */
    List<Content> findAllActiveByIdIn(Collection<Long> contentIds);

    /**
     * Returns whether any content aggregate currently references the contributor.
     */
    boolean existsContributorAssignment(Long contributorId);

    /**
     * Persists the current aggregate state, including nested localizations and story pages.
     */
    Content save(Content content);

    /**
     * Persists and flushes the current aggregate state so subsequent renumbering steps see stable rows.
     */
    Content saveAndFlush(Content content);
}
