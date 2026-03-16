package com.tellpal.v2.content.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContentFreeAccessRepository extends JpaRepository<ContentFreeAccess, Long> {

    List<ContentFreeAccess> findByAccessKey(String accessKey);

    List<ContentFreeAccess> findByContentIdAndLanguageCode(Long contentId, String languageCode);

    Optional<ContentFreeAccess> findByAccessKeyAndContentIdAndLanguageCode(
            String accessKey, Long contentId, String languageCode);

    boolean existsByAccessKeyAndContentIdAndLanguageCode(
            String accessKey, Long contentId, String languageCode);

    void deleteByAccessKeyAndContentIdAndLanguageCode(
            String accessKey, Long contentId, String languageCode);
}
