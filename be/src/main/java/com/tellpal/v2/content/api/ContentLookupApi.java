package com.tellpal.v2.content.api;

import java.util.Optional;

public interface ContentLookupApi {

    Optional<ContentReference> findById(Long contentId);

    Optional<ContentReference> findByExternalKey(String externalKey);
}
