package com.tellpal.v2.content.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ContentRepository {

    Optional<Content> findById(Long id);

    Optional<Content> findByExternalKey(String externalKey);

    boolean existsByExternalKey(String externalKey);

    List<Content> findAllActive();

    List<Content> findAllActiveByIdIn(Collection<Long> contentIds);

    Content save(Content content);
}
