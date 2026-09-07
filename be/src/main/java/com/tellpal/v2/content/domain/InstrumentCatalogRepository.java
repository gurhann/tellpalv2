package com.tellpal.v2.content.domain;

import java.util.Collection;
import java.util.List;

/** Persistence boundary for the managed instrument reference catalog. */
public interface InstrumentCatalogRepository {

    /** Returns active catalog entries with their locale labels in stable code order. */
    List<InstrumentCatalog> findAllActiveOrdered();

    /** Returns catalog entries for the requested codes, including retired rows for validation. */
    List<InstrumentCatalog> findAllByCodeIn(Collection<String> codes);
}
