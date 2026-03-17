package com.tellpal.v2.content.domain;

import java.util.Optional;

public interface ContributorRepository {

    Optional<Contributor> findById(Long id);

    Contributor save(Contributor contributor);
}
