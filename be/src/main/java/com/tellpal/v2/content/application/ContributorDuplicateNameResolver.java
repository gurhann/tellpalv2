package com.tellpal.v2.content.application;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.tellpal.v2.content.domain.ContributorRepository;

/**
 * Resolves a duplicate contributor identity outside a transaction that failed while flushing.
 */
@Service
class ContributorDuplicateNameResolver {

    private final ContributorRepository contributorRepository;

    ContributorDuplicateNameResolver(ContributorRepository contributorRepository) {
        this.contributorRepository = contributorRepository;
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    Optional<Long> findExistingContributorId(String normalizedDisplayName) {
        return contributorRepository.findByNormalizedDisplayName(normalizedDisplayName)
                .map(contributor -> contributor.getId());
    }
}
