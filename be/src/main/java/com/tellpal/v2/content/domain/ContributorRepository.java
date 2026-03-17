package com.tellpal.v2.content.domain;

import java.util.List;
import java.util.Optional;

public interface ContributorRepository {

    Optional<Contributor> findById(Long id);

    List<Contributor> findRecent(int limit);

    Contributor save(Contributor contributor);
}
