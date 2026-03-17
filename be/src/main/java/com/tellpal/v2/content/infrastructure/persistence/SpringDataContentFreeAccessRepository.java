package com.tellpal.v2.content.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tellpal.v2.content.domain.ContentFreeAccess;
import com.tellpal.v2.shared.domain.LanguageCode;

interface SpringDataContentFreeAccessRepository extends JpaRepository<ContentFreeAccess, Long> {

    boolean existsByAccessKey(String accessKey);

    boolean existsByAccessKeyAndContentIdAndLanguageCode(String accessKey, Long contentId, LanguageCode languageCode);

    Optional<ContentFreeAccess> findByAccessKeyAndContentIdAndLanguageCode(
            String accessKey,
            Long contentId,
            LanguageCode languageCode);

    List<ContentFreeAccess> findByAccessKeyOrderByLanguageCodeAscContentIdAsc(String accessKey);

    List<ContentFreeAccess> findByAccessKeyAndLanguageCodeOrderByContentIdAsc(String accessKey, LanguageCode languageCode);
}
