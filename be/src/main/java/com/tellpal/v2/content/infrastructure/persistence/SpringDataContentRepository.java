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

    Optional<Content> findByExternalKey(String externalKey);

    boolean existsByExternalKey(String externalKey);

    List<Content> findAllByActiveTrue();

    List<Content> findAllByActiveTrueAndIdIn(Collection<Long> contentIds);
}
