package com.tellpal.v2.content.infrastructure.persistence;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.tellpal.v2.content.domain.InstrumentCatalog;
import com.tellpal.v2.content.domain.InstrumentCatalogRepository;

/** JPA adapter for the instrument catalog persistence boundary. */
@Repository
public class JpaInstrumentCatalogRepositoryAdapter implements InstrumentCatalogRepository {

    private final SpringDataInstrumentCatalogRepository repository;

    public JpaInstrumentCatalogRepositoryAdapter(SpringDataInstrumentCatalogRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<InstrumentCatalog> findAllActiveOrdered() {
        return repository.findAllActiveOrdered();
    }

    @Override
    public List<InstrumentCatalog> findAllByCodeIn(Collection<String> codes) {
        return repository.findAllByCodeIn(codes);
    }
}
