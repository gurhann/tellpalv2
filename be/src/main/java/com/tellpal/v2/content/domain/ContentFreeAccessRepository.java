package com.tellpal.v2.content.domain;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.tellpal.v2.shared.domain.LanguageCode;

public interface ContentFreeAccessRepository {

    boolean existsByAccessKey(String accessKey);

    boolean existsByAccessKeyAndContentIdAndLanguageCode(String accessKey, Long contentId, LanguageCode languageCode);

    Optional<ContentFreeAccess> findByAccessKeyAndContentIdAndLanguageCode(
            String accessKey,
            Long contentId,
            LanguageCode languageCode);

    List<ContentFreeAccess> findByAccessKey(String accessKey);

    Set<ContentFreeAccess> findByAccessKeyAndLanguageCode(String accessKey, LanguageCode languageCode);

    ContentFreeAccess save(ContentFreeAccess contentFreeAccess);

    void delete(ContentFreeAccess contentFreeAccess);
}
