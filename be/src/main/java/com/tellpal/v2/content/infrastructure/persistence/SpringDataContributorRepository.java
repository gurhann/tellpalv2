package com.tellpal.v2.content.infrastructure.persistence;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tellpal.v2.content.domain.Contributor;

interface SpringDataContributorRepository extends JpaRepository<Contributor, Long> {

    List<Contributor> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("""
            select contributor
            from Contributor contributor
            where lower(contributor.displayName) like lower(concat('%', :query, '%'))
            order by contributor.displayName asc, contributor.id asc
            """)
    List<Contributor> searchByDisplayName(@Param("query") String query, Pageable pageable);
}
