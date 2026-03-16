package com.tellpal.v2.content.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContentContributorRepository extends JpaRepository<ContentContributor, Long> {

    List<ContentContributor> findByContentIdOrderBySortOrderAsc(Long contentId);

    List<ContentContributor> findByContentIdAndRole(Long contentId, ContributorRole role);

    void deleteByContentIdAndContributorId(Long contentId, Long contributorId);
}
