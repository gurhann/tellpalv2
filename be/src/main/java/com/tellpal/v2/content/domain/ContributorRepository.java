package com.tellpal.v2.content.domain;

import java.util.List;
import java.util.Optional;

/**
 * Stores contributor records that can later be linked to localized content.
 */
public interface ContributorRepository {

    /**
     * Returns the contributor for the internal id when it exists.
     */
    Optional<Contributor> findById(Long id);

    /**
     * Returns the most recently created or updated contributors for admin selection flows.
     */
    List<Contributor> findRecent(int limit);

    /**
     * Deletes one contributor profile.
     */
    void delete(Contributor contributor);

    /**
     * Persists contributor profile changes.
     */
    Contributor save(Contributor contributor);
}
