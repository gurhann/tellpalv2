package com.tellpal.v2.content.domain;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.tellpal.v2.shared.domain.LanguageCode;

/**
 * Persists free-access grants that make localized content reachable through an access key.
 */
public interface ContentFreeAccessRepository {

    /**
     * Checks whether the access key is already in use by any localized grant.
     */
    boolean existsByAccessKey(String accessKey);

    /**
     * Checks whether the exact access-key and localized-content combination already exists.
     */
    boolean existsByAccessKeyAndContentIdAndLanguageCode(String accessKey, Long contentId, LanguageCode languageCode);

    /**
     * Returns the grant for the exact access-key and localized-content combination when present.
     */
    Optional<ContentFreeAccess> findByAccessKeyAndContentIdAndLanguageCode(
            String accessKey,
            Long contentId,
            LanguageCode languageCode);

    /**
     * Returns every localized grant covered by the same externally shared access key.
     */
    List<ContentFreeAccess> findByAccessKey(String accessKey);

    /**
     * Returns localized grants that match the requested language for access resolution.
     */
    Set<ContentFreeAccess> findByAccessKeyAndLanguageCode(String accessKey, LanguageCode languageCode);

    /**
     * Persists a free-access grant after validation has already been enforced by the aggregate logic.
     */
    ContentFreeAccess save(ContentFreeAccess contentFreeAccess);

    /**
     * Removes a previously issued free-access grant.
     */
    void delete(ContentFreeAccess contentFreeAccess);
}
