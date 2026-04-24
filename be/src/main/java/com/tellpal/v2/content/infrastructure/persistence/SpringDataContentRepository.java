package com.tellpal.v2.content.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.tellpal.v2.content.domain.Content;

interface SpringDataContentRepository extends JpaRepository<Content, Long> {

    @Query("""
            select distinct content
            from Content content
            left join fetch content.localizations
            order by content.id asc
            """)
    List<Content> findAllForAdminRead();

    @Query("""
            select distinct content
            from Content content
            left join fetch content.localizations
            where content.id = :id
            """)
    Optional<Content> findByIdForAdminRead(Long id);

    @Query("""
            select distinct content
            from Content content
            left join fetch content.localizations
            where content.id in :ids
            order by content.id asc
            """)
    List<Content> findAllByIdForAdminReadIn(Collection<Long> ids);

    @Query("""
            select distinct content
            from Content content
            left join fetch content.storyPages storyPage
            left join fetch storyPage.localizations
            where content.id = :id
            """)
    Optional<Content> findByIdForStoryPageAdminRead(Long id);

    @Query("""
            select distinct content
            from Content content
            left join fetch content.contributors assignment
            left join fetch assignment.contributor
            where content.id = :id
            """)
    Optional<Content> findByIdForContributorAdminRead(Long id);

    Optional<Content> findByExternalKey(String externalKey);

    boolean existsByExternalKey(String externalKey);

    List<Content> findAllByActiveTrue();

    List<Content> findAllByActiveTrueAndIdIn(Collection<Long> contentIds);

    @Query("""
            select count(assignment) > 0
            from Content content
            join content.contributors assignment
            where assignment.contributor.id = :contributorId
            """)
    boolean existsContributorAssignment(Long contributorId);
}
