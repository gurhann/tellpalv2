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

    /** Locks one profile while a role-sensitive mutation is in progress. */
    Optional<Contributor> findByIdForWrite(Long id);

    Optional<Contributor> findByNormalizedDisplayName(String normalizedDisplayName);

    /**
     * Returns the most recently created or updated contributors for admin selection flows.
     */
    List<Contributor> findRecent(int limit);

    /**
     * Searches contributors by display name for admin selection flows.
     */
    List<Contributor> searchByDisplayName(String query, int limit);

    /** Returns the requested registry page after applying database-side filters and ordering. */
    ContributorRegistryPage findRegistryPage(String query, ContributorRole role, int page, int size);

    /** Project-owned page result keeps persistence framework types out of the domain port. */
    record ContributorRegistryPage(List<Contributor> contributors, long totalItems) {
        public ContributorRegistryPage {
            contributors = contributors == null ? List.of() : List.copyOf(contributors);
        }
    }

    /**
     * Deletes one contributor profile.
     */
    void delete(Contributor contributor);

    /**
     * Persists contributor profile changes.
     */
    Contributor save(Contributor contributor);

    Contributor saveAndFlush(Contributor contributor);
}
