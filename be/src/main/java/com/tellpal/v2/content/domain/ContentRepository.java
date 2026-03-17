package com.tellpal.v2.content.domain;

import java.util.Optional;

public interface ContentRepository {

    Optional<Content> findById(Long id);

    Optional<Content> findByExternalKey(String externalKey);

    boolean existsByExternalKey(String externalKey);

    Content save(Content content);
}
