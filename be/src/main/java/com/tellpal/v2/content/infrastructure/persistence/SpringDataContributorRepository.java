package com.tellpal.v2.content.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import com.tellpal.v2.content.domain.Contributor;
import com.tellpal.v2.content.domain.ContributorRole;

interface SpringDataContributorRepository extends JpaRepository<Contributor, Long> {

    Optional<Contributor> findByNormalizedDisplayName(String normalizedDisplayName);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select contributor from Contributor contributor where contributor.id = :id")
    Optional<Contributor> findByIdForWrite(Long id);

    List<Contributor> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("""
            select contributor
            from Contributor contributor
            where lower(contributor.displayName) like lower(concat('%', :query, '%'))
            order by contributor.displayName asc, contributor.id asc
            """)
    List<Contributor> searchByDisplayName(@Param("query") String query, Pageable pageable);

    @Query(value = """
            select contributor from Contributor contributor
            where (:query = '' or lower(contributor.displayName) like concat('%', lower(:query), '%') escape '\\')
              and (:role is null or :role member of contributor.roles)
            order by contributor.updatedAt desc, contributor.id desc
            """, countQuery = """
            select count(contributor) from Contributor contributor
            where (:query = '' or lower(contributor.displayName) like concat('%', lower(:query), '%') escape '\\')
              and (:role is null or :role member of contributor.roles)
            """)
    Page<Contributor> findRegistryPage(@Param("query") String query, @Param("role") ContributorRole role,
            Pageable pageable);
}
