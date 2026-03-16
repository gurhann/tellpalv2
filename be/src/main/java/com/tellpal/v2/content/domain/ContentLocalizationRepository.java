package com.tellpal.v2.content.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContentLocalizationRepository extends JpaRepository<ContentLocalization, ContentLocalizationId> {

    List<ContentLocalization> findByContentId(Long contentId);

    Optional<ContentLocalization> findByContentIdAndLanguageCode(Long contentId, String languageCode);
}
