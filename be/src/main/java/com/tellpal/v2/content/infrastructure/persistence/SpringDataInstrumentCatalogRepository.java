package com.tellpal.v2.content.infrastructure.persistence;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.tellpal.v2.content.domain.InstrumentCatalog;

interface SpringDataInstrumentCatalogRepository extends JpaRepository<InstrumentCatalog, Long> {

    @Query("""
            select distinct catalog
            from InstrumentCatalog catalog
            left join fetch catalog.localizations
            where catalog.active = true
            order by catalog.code asc
            """)
    List<InstrumentCatalog> findAllActiveOrdered();

    @Query("""
            select distinct catalog
            from InstrumentCatalog catalog
            left join fetch catalog.localizations
            where catalog.code in :codes
            """)
    List<InstrumentCatalog> findAllByCodeIn(Collection<String> codes);
}
